package cam72cam.immersiverailroading.physics.simulation;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Vector3f;
import util.Matrix4;

import java.nio.FloatBuffer;
import java.util.*;

/** Nothing here should be considered thread safe! **/
public class RigidBodyBox {
    private final Vector3f[] points;
    private final float x2;
    private final float y2;
    private final float z2;
    private float restitution = 0.7f;

    private float massKg;
    private float inverseMassKg;
    private Matrix3f inverseInternalInertiaTensor;

    private int stateId = 0;
    private State[] states;

    public static Map<Vec3i, Boolean> airPositions = new HashMap<>();
    private double radius;

    public class State {
        // coordinates
        private Vector3f position = new Vector3f(); // linear
        private Matrix3f orientation = new Matrix3f(); // angular

        // velocity
        private Vector3f linearVelocity = new Vector3f(); // linear
        private Vector3f angularVelocity = new Vector3f(); // angular

        // forces
        private Vector3f force = new Vector3f(); // linear
        private Vector3f torque = new Vector3f(); // angular

        private Matrix3f inverseExternalInertiaTensor = new Matrix3f();
        private Vector3f angularMomentum = new Vector3f();

        private Vector3f[] calculatedPoints;

        private State() {
            calculatedPoints = new Vector3f[points.length];
            for (int i = 0; i < points.length; i++) {
                calculatedPoints[i] = new Vector3f();
            }
        }

        public void setOrientation(float degX, float degY, float degZ) {
            MathUtils.setRotation(degX, degY, degZ, orientation);
        }

        public Vector3f getOrientation() {
            Vector3f result = new Vector3f(); // TODO remove this allocation if possible
            MathUtils.getRotation(orientation, result);
            return result;
        }

        public void setPosition(Vec3d pos) {
            position.x = (float) pos.x;
            position.y = (float) pos.y;
            position.z = (float) pos.z;
        }

        public Vec3d getPosition() {
            return new Vec3d(
                    position.x,
                    position.y,
                    position.z
            );
        }

        public Matrix4 getMatrix() {
            return MathUtils.createTransformationMatrix(orientation, position);
        }

        public void resetForces() {
            // Clear forces that have been already applied
            MathUtils.zero(torque);
            MathUtils.zero(force);
        }

        public void computeForces() {
            // Gravity
            force.y += -9.8 * massKg;

            // Damping?
            float kdLinear = -0.02f;
            float kdAngular = -0.02f;
            MathUtils.addScaledTo(linearVelocity, kdLinear, force);
            MathUtils.addScaledTo(angularVelocity, kdAngular, torque);

            //testing
            float delta = (float) Math.toRadians(20);
            /*
            angularVelocity.x = delta;
            angularVelocity.y = delta;
            angularVelocity.z = delta;
             */
        }

        public void integrate(State previous, float elapsedTime) {
            // Copy position from previous
            MathUtils.copyInto(previous.position, this.position);

            // Apply linear velocity
            // position (m) += linearVelocity (m/s) * elapsedTime (1/s)
            MathUtils.addScaledTo(previous.linearVelocity, elapsedTime, this.position);


            // Apply angular velocity
            // orientation = skew(angularVelocity) (ang/s)
            MathUtils.skewSymetric(previous.angularVelocity, this.orientation);
            // orientation = skew(angularVelocity) (ang/s) * orientation (ang)
            Matrix3f.mul(this.orientation, previous.orientation, this.orientation);
            // orientation = (skew(angularVelocity) (ang/s) * orientation (ang)) * elapsedTime (1/s)
            MathUtils.scaleMatrix(this.orientation, elapsedTime);
            // orientation = orientation + ((skew(angularVelocity) (ang/s) * orientation (ang)) * elapsedTime (1/s))
            Matrix3f.add(previous.orientation, this.orientation, this.orientation);

            // Magic
            MathUtils.orthnormalize(this.orientation);

            // Apply linear acceleration (force/mass)
            // linearVelocity (m/s) += force (kg * m/s/s) * inverseMass (1/kg) * elapsedTime (1/s)
            MathUtils.copyInto(previous.linearVelocity, this.linearVelocity);
            MathUtils.addScaledTo(previous.force, inverseMassKg * elapsedTime, this.linearVelocity);

            // Apply angular acceleration (torque/mass)
            // angularMomentum (kg * ang/s) += torque (kg * ang/s/s) * elapsedTime (1/s)
            MathUtils.copyInto(previous.angularMomentum, this.angularMomentum);
            MathUtils.addScaledTo(previous.torque, elapsedTime, this.angularMomentum);

            // Apply tensor within orientation view
            // inverseExternalInertiaTensor = orientation * inverseInternalInertiaTensor
            Matrix3f.mul(this.orientation, inverseInternalInertiaTensor, this.inverseExternalInertiaTensor);
            // inverseExternalInertiaTensor = orientation * inverseInternalInertiaTensor * transpose(orientation)
            this.orientation.transpose();
            Matrix3f.mul(this.inverseExternalInertiaTensor, this.orientation, this.inverseExternalInertiaTensor);
            this.orientation.transpose();

            // angularVelocity (ang/s) = inverseExternalInertiaTensor (1/kg) * angularMomentum (kg * ang/s)
            MathUtils.someOperation(this.inverseExternalInertiaTensor, this.angularMomentum, this.angularVelocity);
        }

        public void updateVertices() {
            for (int i = 0; i < points.length; i++) {
                Vector3f outPoint = calculatedPoints[i];
                // This is wrong... pitch / roll flipped!
                //MathUtils.someOperation(orientation, points[i], outPoint);
                Matrix3f.transform(orientation, points[i], outPoint);
                Vector3f.add(outPoint, position, outPoint);
            }
        }

        public List<Runnable> collideWithOther(RigidBodyBox other) {
            float epsilon = 0.01f;

            State otherState = other.currentState();
            float centerDistance = Vector3f.sub(otherState.position, this.position, new Vector3f()).lengthSquared();
            if (centerDistance > Math.pow(other.radius + radius, 2)) {
                return Collections.emptyList();
            }
            List<Runnable> results = new ArrayList<>();

            for (Vector3f calculatedPoint : calculatedPoints) {
                Vector3f pointRelativeToOther = Vector3f.sub(calculatedPoint, otherState.position, new Vector3f());
                // Apply inverse transform to relativePointOther
                otherState.orientation.invert();
                Vector3f pointInOtherSpace = Matrix3f.transform(otherState.orientation, pointRelativeToOther, new Vector3f());
                otherState.orientation.invert();

                if (Math.abs(pointInOtherSpace.x) < other.x2 && Math.abs(pointInOtherSpace.y) < other.y2 && Math.abs(pointInOtherSpace.z) < other.z2) {
                    //System.out.println("PENETRATE!");
                    Vector3f relativePoint = Vector3f.sub(calculatedPoint, position, new Vector3f());

                    Vector3f velocity = new Vector3f();
                    Vector3f.add(velocity, Vector3f.cross(angularVelocity, relativePoint, new Vector3f()), velocity);
                    Vector3f.add(velocity, linearVelocity, velocity);
                    Vector3f.sub(velocity, Vector3f.cross(otherState.angularVelocity, pointRelativeToOther, new Vector3f()), velocity);
                    Vector3f.sub(velocity, otherState.linearVelocity, velocity);

                    // Find closest wall
                    float xNeg = Math.abs(pointInOtherSpace.x + other.x2);
                    float xPos = Math.abs(pointInOtherSpace.x - other.x2);
                    float yNeg = Math.abs(pointInOtherSpace.y + other.y2);
                    float yPos = Math.abs(pointInOtherSpace.y - other.y2);
                    float zNeg = Math.abs(pointInOtherSpace.z + other.z2);
                    float zPos = Math.abs(pointInOtherSpace.z - other.z2);
                    float[] wallDistances = new float[]{
                            xNeg,
                            xPos,
                            yNeg,
                            yPos,
                            zNeg,
                            zPos,
                    };
                    float minWall = xNeg;
                    for (float wallDistance : wallDistances) {
                        minWall = Math.min(minWall, wallDistance);
                    }
                    Vector3f normal;
                    if (minWall == xNeg) {
                        normal = new Vector3f(-1, 0, 0);
                    } else if (minWall == xPos) {
                        normal = new Vector3f(1, 0, 0);
                    } else if (minWall == yNeg) {
                        normal = new Vector3f(0, -1, 0);
                    } else if (minWall == yPos) {
                        normal = new Vector3f(0, 1, 0);
                    } else if (minWall == zNeg) {
                        normal = new Vector3f(0, 0, -1);
                    } else if (minWall == zPos) {
                        normal = new Vector3f(0, 0, 1);
                    } else {
                        throw new RuntimeException("UNREACHABLE");
                    }
                    //System.out.println(calculatedPoint);

                    //System.out.println(normal);
                    Matrix3f.transform(otherState.orientation, normal, normal);
                    //System.out.println("NORMAL " + normal);

                    /*float axbyczd = Vector3f.dot(calculatedPoint, normal);
                    if (axbyczd < epsilon) {
                        System.out.println("collide " + axbyczd);
                    }*/
                    float relativeVelocity = Vector3f.dot(normal, velocity);
                    if (relativeVelocity < 0) {
                        // collided
                        //System.out.println("IMPULSE " + relativeVelocity);
                        results.add(() -> {
                            // TODO remove this recalc???
                            MathUtils.zero(velocity);
                            Vector3f.add(velocity, Vector3f.cross(this.angularVelocity, relativePoint, new Vector3f()), velocity);
                            Vector3f.add(velocity, this.linearVelocity, velocity);
                            Vector3f.sub(velocity, Vector3f.cross(otherState.angularVelocity, pointRelativeToOther, new Vector3f()), velocity);
                            Vector3f.sub(velocity, otherState.linearVelocity, velocity);


                            float impulseNumerator = -(1 + Math.min(other.restitution, restitution)) *
                                    Vector3f.dot(velocity, normal);
                            float impulseDenominator = (inverseMassKg + other.inverseMassKg);
                            float impulseDenominatorSelf = Vector3f.dot(Vector3f.cross(
                                                    MathUtils.someOperation(
                                                            inverseExternalInertiaTensor,
                                                            Vector3f.cross(relativePoint, normal, new Vector3f()),
                                                            new Vector3f()
                                                    ),
                                                    relativePoint,
                                                    new Vector3f()
                                            ),
                                            normal
                                    );
                            float impulseDenominatorOther = Vector3f.dot(Vector3f.cross(
                                            MathUtils.someOperation(
                                                    otherState.inverseExternalInertiaTensor,
                                                    Vector3f.cross(pointRelativeToOther, normal, new Vector3f()),
                                                    new Vector3f()
                                            ),
                                            pointRelativeToOther,
                                            new Vector3f()
                                    ),
                                    normal
                            );

                            Vector3f impulseSelf = new Vector3f(normal);
                            Vector3f impulseOther = new Vector3f(normal);
                            impulseSelf.scale(impulseNumerator / (impulseDenominator + impulseDenominatorSelf));
                            impulseOther.scale(-impulseNumerator / (impulseDenominator + impulseDenominatorOther));

                            MathUtils.addScaledTo(impulseSelf, inverseMassKg, linearVelocity);
                            MathUtils.addScaledTo(impulseOther, other.inverseMassKg, otherState.linearVelocity);
                            Vector3f.add(this.angularMomentum, Vector3f.cross(relativePoint, impulseSelf, new Vector3f()), this.angularMomentum);
                            Vector3f.add(otherState.angularMomentum, Vector3f.cross(pointRelativeToOther, impulseOther, new Vector3f()), otherState.angularMomentum);

                            MathUtils.someOperation(this.inverseExternalInertiaTensor, this.angularMomentum, this.angularVelocity);
                            MathUtils.someOperation(otherState.inverseExternalInertiaTensor, otherState.angularMomentum, otherState.angularVelocity);

                            impulseSelf.scale(inverseMassKg);
                            impulseOther.scale(other.inverseMassKg);
                            //System.out.println("SELF:  " + impulseSelf);
                            //System.out.println("OTHER: " + impulseOther);
                        });
                    }
                }
            }

            return results;
        }

        // TODO no allocations
        public List<Runnable> collideWithWorld(World world) {
            if (1 == 0) {
                return Collections.emptyList();
            }
            List<Runnable> results = new ArrayList<>();
            float epsilon = 0.01f;

            for (int i = 0; i < calculatedPoints.length; i++) {
                Vector3f calculatedPoint = calculatedPoints[i];

                Vec3i blockPos = new Vec3i(calculatedPoint.x, calculatedPoint.y, calculatedPoint.z).up();
                while (blockPos.y > 1) {
                    if (!airPositions.computeIfAbsent(blockPos, ph -> world.isAir(ph) || world.isBlock(ph, IRBlocks.BLOCK_RAIL_GAG) || world.isBlock(ph, IRBlocks.BLOCK_RAIL))) {
                        break;
                    }
                    blockPos = blockPos.down();
                }
                blockPos = blockPos.up();
                // angle from origin to colliding point
                Vector3f relativePoint = Vector3f.sub(calculatedPoint, position, new Vector3f());

                Vector3f velocity = new Vector3f();
                Vector3f.add(linearVelocity, Vector3f.cross(angularVelocity, relativePoint, velocity), velocity);

                float axbyczd = Vector3f.dot(calculatedPoint, worldNorm) - blockPos.y;
                if (axbyczd < 1) {
                    //System.out.println(axbyczd);
                }
                /*if (axbyczd < -epsilon) {
                    System.out.printf("PEN %s: %s %s%n", i, calculatedPoint, velocity);
                    // We hit, but can't resolve this deep in
                    return null;
                } else */ if (axbyczd < epsilon) {
                    //System.out.printf("COL %s: %s %s%n", i, calculatedPoint, velocity);


                    float relativeVelocity = Vector3f.dot(worldNorm, velocity);
                    if (relativeVelocity < 0) {
                        // collided
                        //System.out.println("IMPULSE");
                        results.add(() -> {
                            Vector3f.add(linearVelocity, Vector3f.cross(angularVelocity, relativePoint, velocity), velocity);


                            float impulseNumerator = -(1 + restitution) *
                                    Vector3f.dot(velocity, worldNorm);
                            float impulseDenominator = inverseMassKg +
                                    Vector3f.dot(Vector3f.cross(
                                                    MathUtils.someOperation(
                                                            inverseExternalInertiaTensor,
                                                            Vector3f.cross(relativePoint, worldNorm, new Vector3f()),
                                                            new Vector3f()
                                                    ),
                                                    relativePoint,
                                                    new Vector3f()
                                            ),
                                            worldNorm
                                    );

                            Vector3f impulse = new Vector3f(worldNorm);
                            impulse.scale(impulseNumerator / impulseDenominator);

                            MathUtils.addScaledTo(impulse, inverseMassKg, linearVelocity);
                            Vector3f.add(angularMomentum, Vector3f.cross(relativePoint, impulse, new Vector3f()), angularMomentum);

                            //System.out.println(new Vector3f(impulse).scale(inverseMassKg));
                            //System.out.println("LinearVelocity: " + linearVelocity);
                            //System.out.println("Angular Momentum: " + angularMomentum);

                            MathUtils.someOperation(inverseExternalInertiaTensor, angularMomentum, angularVelocity);
                        });
                    }
                }
            }
            return results;
        }

        public void addInternalLinearForce(float newtons, Vec3d direction) {
            Vector3f f = new Vector3f((float) direction.x, (float) direction.y, (float) direction.z);
            f.normalise();
            f.scale(newtons);
            Matrix3f.transform(orientation, f, f);
            Vector3f.add(f, force, force);

        }
    }

    public RigidBodyBox(float length, float width, float height, float massKg) {
        x2 = length/2;
        y2 = height/2;
        z2 = width/2;

        points = new Vector3f[(int) (Math.floor(x2/2 + x2/2 + 1) * Math.floor(y2/2 + y2/2 + 1) * Math.floor(z2/2 + z2/2 + 1))];
        try {
            int i = 0;
            for (float x = -x2; x <= x2; x+=2) {
                if (x > x2) {
                    x = x2;
                }
                for (float y = -y2; y <= y2; y+=2) {
                    if (y > y2) {
                        y = y2;
                    }
                    for (float z = -z2; z <= z2; z+=2) {
                        if (z > z2) {
                            z = z2;
                        }
                        points[i] = new Vector3f(x, y, z);
                        i++;
                    }
                }

            }
        } catch (Exception ex) {
            ex.printStackTrace();
            /*
            points = new Vector3f[] {
                    new Vector3f(x2, y2, z2),
                    new Vector3f(x2, y2, -z2),
                    new Vector3f(x2, -y2, z2),
                    new Vector3f(x2, -y2, -z2),
                    new Vector3f(-x2, y2, z2),
                    new Vector3f(-x2, y2, -z2),
                    new Vector3f(-x2, -y2, z2),
                    new Vector3f(-x2, -y2, -z2),
            };*/
        }

        radius = Math.max(x2, Math.max(y2, z2));

        this.massKg = massKg;
        this.inverseMassKg = 1f/massKg;
        this.inverseInternalInertiaTensor = new Matrix3f();
        this.inverseInternalInertiaTensor.m00 = (3f / (y2*y2 + z2*z2)) * inverseMassKg;
        this.inverseInternalInertiaTensor.m11 = (3f / (x2*x2 + z2*z2)) * inverseMassKg;
        this.inverseInternalInertiaTensor.m22 = (3f / (x2*x2 + y2*y2)) * inverseMassKg;

        this.stateId = 0;
        this.states = new State[] {
                new State(),
                new State()
        };
    }

    public void setRestitution(float restitution) {
        this.restitution = restitution;
    }

    public void setMassKg(float massKg) {
        if (massKg != this.massKg) {
            inverseInternalInertiaTensor.m00 *= this.massKg; // cancel out mass
            inverseInternalInertiaTensor.m11 *= this.massKg; // cancel out mass
            inverseInternalInertiaTensor.m22 *= this.massKg; // cancel out mass

            this.massKg = massKg;
            this.inverseMassKg = 1f / massKg;

            inverseInternalInertiaTensor.m00 *= inverseMassKg; // factor mass back in
            inverseInternalInertiaTensor.m11 *= inverseMassKg; // factor mass back in
            inverseInternalInertiaTensor.m22 *= inverseMassKg; // factor mass back in
        }
    }

    public State currentState() {
        return states[stateId];
    }
    public State previousState() {
        return states[stateId == 0 ? 1 : 0];
    }
    public void nextState() {
        stateId = stateId == 0 ? 1 : 0;
    }

    private static final Vector3f worldNorm = new Vector3f(0, 1, 0);
    public List<Runnable> collideWithWorld(World world) {
        return currentState().collideWithWorld(world);
    }

    public Vector3f[] getPoints() {
        return currentState().calculatedPoints;
    }

    public List<Runnable> collideWithOther(RigidBodyBox rigidBodyBox) {
        return this.currentState().collideWithOther(rigidBodyBox);
    }


    public void addRotation(int degX, int degY, int degZ) {
        Matrix3f temp = new Matrix3f();
        MathUtils.setRotation(degX, degY, degZ, temp);
        Matrix3f.mul(temp, currentState().orientation, currentState().orientation);
    }

    private static class MathUtils {
        public static Matrix4 createTransformationMatrix(Matrix3f orientation, Vector3f position) {
            Matrix4 m = new Matrix4();
            m.m00 = orientation.m00;
            m.m01 = orientation.m01;
            m.m02 = orientation.m02;
            m.m10 = orientation.m10;
            m.m11 = orientation.m11;
            m.m12 = orientation.m12;
            m.m20 = orientation.m20;
            m.m21 = orientation.m21;
            m.m22 = orientation.m22;

            /*
            m.m03 = 1;
            m.m13 = 1;
            m.m23 = 1;
            m.m33 = 1;

             */

            m.m30 = position.x;
            m.m31 = position.y;
            m.m32 = position.z;

            return m;
        }

        // https://learnopencv.com/rotation-matrix-to-euler-angles/
        private static final Matrix3f R_x = new Matrix3f();
        private static final Matrix3f R_y = new Matrix3f();
        private static final Matrix3f R_z = new Matrix3f();
        public static void setRotation(float degX, float degY, float degZ, Matrix3f out) {
            double rx = Math.toRadians(degX);
            double ry = Math.toRadians(degY);
            double rz = Math.toRadians(degZ);

            float cx = (float) Math.cos(rx);
            float sx = (float) Math.sin(rx);
            float cy = (float) Math.cos(ry);
            float sy = (float) Math.sin(ry);
            float cz = (float) Math.cos(rz);
            float sz = (float) Math.sin(rz);


            // TODO get rid of FloatBuffer.wrap

            // Calculate rotation about x axis
            R_x.load(FloatBuffer.wrap(new float[]{
                    1, 0, 0,
                    0, cx, -sx,
                    0, sx, cx
            }));

            // Calculate rotation about y axis
            R_y.load(FloatBuffer.wrap(new float[]{
                    cy, 0, sy,
                    0, 1, 0,
                    -sy, 0, cy
            }));

            // Calculate rotation about z axis
            R_z.load(FloatBuffer.wrap(new float[]{
                    cz, -sz, 0,
                    sz, cz, 0,
                    0, 0, 1
            }));

            // Combined rotation matrix
            //noinspection SuspiciousNameCombination
            Matrix3f.mul(R_y, Matrix3f.mul(R_z, R_x, out), out);
        }

        public static void getRotation(Matrix3f orientation, Vector3f out) {
            orientation.transpose();
            out.y = (float) Math.atan2(-orientation.m20, orientation.m00);
            out.z = (float) Math.atan2(orientation.m10, Math.sqrt(1 - Math.pow(orientation.m10, 2)));
            out.x = (float) Math.atan2(-orientation.m12, orientation.m11);

            out.x = (float) Math.toDegrees(out.x);
            out.y = (float) Math.toDegrees(out.y);
            out.z = (float) Math.toDegrees(out.z);
            orientation.transpose();
        }

        public static void addScaledTo(Vector3f source, float scale, Vector3f dest) {
            dest.x += source.x * scale;
            dest.y += source.y * scale;
            dest.z += source.z * scale;
        }

        public static void scaleMatrix(Matrix3f matrix, float scale) {
            matrix.m00 *= scale;
            matrix.m01 *= scale;
            matrix.m02 *= scale;
            matrix.m10 *= scale;
            matrix.m11 *= scale;
            matrix.m12 *= scale;
            matrix.m20 *= scale;
            matrix.m21 *= scale;
            matrix.m22 *= scale;
        }

        public static void skewSymetric(Vector3f source, Matrix3f dest) {
            dest.m00 = 0;
            dest.m01 = -source.z;
            dest.m02 = source.y;
            dest.m10 = source.z;
            dest.m11 = 0;
            dest.m12 = -source.x;
            dest.m20 = -source.y;
            dest.m21 = source.x;
            dest.m22 = 0;
            dest.transpose();
        }

        public static void zero(Vector3f vec) {
            vec.x = vec.y = vec.z = 0;
        }

        private static final Vector3f X = new Vector3f();
        private static final Vector3f Y = new Vector3f();
        private static final Vector3f Z = new Vector3f();
        @SuppressWarnings("SuspiciousNameCombination")
        private static void orthnormalize(Matrix3f mat) {
            X.x = mat.m00; X.y = mat.m10; X.z = mat.m20;
            X.normalise();
            Y.x = mat.m01;  Y.y = mat.m11; Y.z = mat.m21;

            Vector3f.cross(X, Y, Z).normalise();
            Vector3f.cross(Z, X, Y).normalise();

            mat.m00 = X.x; mat.m01 = Y.x; mat.m02 = Z.x;
            mat.m10 = X.y; mat.m11 = Y.y; mat.m12 = Z.y;
            mat.m20 = X.z; mat.m21 = Y.z; mat.m22 = Z.z;
        }

        public static void copyInto(Vector3f source, Vector3f dest) {
            dest.x = source.x;
            dest.y = source.y;
            dest.z = source.z;
        }

        public static Vector3f someOperation(Matrix3f left, Vector3f right, Vector3f dest) {
            return Matrix3f.transform(left, right, dest);
            /*dest.x = left.m00 * right.x + left.m01 * right.y + left.m02 * right.z;
            dest.y = left.m10 * right.x + left.m11 * right.y + left.m12 * right.z;
            dest.z = left.m20 * right.x + left.m21 * right.y + left.m22 * right.z;
            return dest;
             */
        }
    }
}
