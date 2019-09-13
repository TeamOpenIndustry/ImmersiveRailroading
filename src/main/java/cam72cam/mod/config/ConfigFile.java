package cam72cam.mod.config;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;

public class ConfigFile {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Name {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Comment {
        String value();
    }

    public static void sync(Class cls) {
        PropertyClass pc = new PropertyClass(cls);
        pc.write().forEach(System.out::println);
        pc.read(pc.write());
    }

    private static Map<Class<?>, Function<Object, List<String>>> encoders = new HashMap<>();
    private static Map<Class<?>, Function<List<String>, Object>> decoders = new HashMap<>();

    public static <T> void addMapper(Class<T> cls, Function<T, List<String>> encoder, Function<List<String>, T> decoder) {
        encoders.put(cls, x -> encoder.apply((T)x));
        decoders.put(cls, x -> decoder.apply(x));
    }

    private static <T> List<T> listOf(T... instance) {
        List<T> r = new ArrayList<>();
        Collections.addAll(r, instance);
        return r;
    }

    static {
        addMapper(int.class, i -> listOf(i.toString()), l -> Integer.parseInt(l.get(0)));
        addMapper(Integer.class, i -> listOf(i == null ? "" : i.toString()), l -> Integer.parseInt(l.get(0)));
        addMapper(long.class, i -> listOf(i.toString()), l -> Long.parseLong(l.get(0)));
        addMapper(Long.class, i -> listOf(i == null ? "" : i.toString()), l -> Long.parseLong(l.get(0)));
        addMapper(float.class, i -> listOf(i.toString()), l -> Float.parseFloat(l.get(0)));
        addMapper(Float.class, i -> listOf(i == null ? "" : i.toString()), l -> Float.parseFloat(l.get(0)));
        addMapper(double.class, i -> listOf(i.toString()), l -> Double.parseDouble(l.get(0)));
        addMapper(Double.class, i -> listOf(i == null ? "" : i.toString()), l -> Double.parseDouble(l.get(0)));

        addMapper(String.class, i -> listOf(i == null ? "" : i), l -> l.get(0));
    }

    private abstract static class Property {
        protected abstract <A extends Annotation> A getAnnotation(Class<A> cls);
        protected abstract List<Property> getSubProperties();

        protected abstract void read(List<String> lines);
        protected abstract List<String> write();

        protected abstract String getName();

        protected String getName(String def) {
            Name n = getAnnotation(Name.class);
            return n == null ? def : n.value();
        }

        protected String getComment() {
            Comment n = getAnnotation(Comment.class);
            return n == null ? "" : n.value();
        }
    }

    private static class PropertyField extends Property {
        private final Field field;

        private PropertyField(Field f) {
            this.field = f;
        }
        @Override
        protected <A extends Annotation> A getAnnotation(Class<A> cls) {
            return field.getAnnotation(cls);
        }

        @Override
        protected List<Property> getSubProperties() {
            return new ArrayList<>();
        }

        @Override
        protected void read(List<String> lines) {
            String name = getName();
            for (String line : lines) {
                if (line.startsWith(name + "=")) {
                    //val = line.replace(name + "=", "");
                }
            }
        }

        @Override
        protected List<String> write() {
            List<String> lines = new ArrayList<>();
            if (getComment().length() > 0) {
                lines.add("# " + getComment());
            }
            List<String> subLines;
            try {
                subLines = encoders.get(field.getType()).apply(field.get(null));
            } catch (IllegalAccessException|NullPointerException e) {
                throw new RuntimeException("Error writing field " + field, e);
            }
            lines.add(getName() + "=" + subLines.get(0));
            for (int i = 1; i < subLines.size(); i++) {
                lines.add(subLines.get(i));
            }
            lines.add("");
            return lines;
        }

        @Override
        protected String getName() {
            return getName(field.getName());
        }
    }

    private static class PropertyClass extends Property {

        private final Class<?> cls;
        private final List<Property> properties;

        public PropertyClass(Class<?> cls) {
            this.cls = cls;
            this.properties = new ArrayList<>();

            for (Field field : cls.getDeclaredFields()) {
                field.setAccessible(true);
                if (canAccess(field.getModifiers()) && field.isAccessible()) {
                    properties.add(new PropertyField(field));
                }
            }

            for (Class<?> scls : cls.getDeclaredClasses()) {
                if (Modifier.isPublic(cls.getModifiers())) {
                    properties.add(new PropertyClass(scls));
                }
            }
        }

        @Override
        protected <A extends Annotation> A getAnnotation(Class<A> cls) {
            return this.cls.getAnnotation(cls);
        }

        @Override
        protected List<Property> getSubProperties() {
            return properties;
        }

        @Override
        protected void read(List<String> lines) {
            // TODO
        }

        @Override
        protected List<String> write() {
            List<String> lines = new ArrayList<>();
            if (getComment().length() > 0) {
                lines.add("# " + getComment());
            }
            lines.add(getName() + " {");
            for (Property p : properties) {
                p.write().forEach(line -> lines.add("    " + line));
            }
            lines.add("}");
            lines.add("");
            return lines;
        }

        @Override
        protected String getName() {
            return getName(cls.getSimpleName());
        }

        private boolean canAccess(int modifiers) {
            return Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers) && !Modifier.isFinal(modifiers);
        }
    }
}
