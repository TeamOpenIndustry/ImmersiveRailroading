varying float DEPTH;
uniform float ALPHA;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    gl_TexCoord[0] = gl_MultiTexCoord0;
    DEPTH = gl_Position.z / 100.0;
    if (DEPTH > 1.0) {
    	DEPTH = 1.0;
    }
    DEPTH = (1.0-DEPTH);
    DEPTH = ALPHA;
}