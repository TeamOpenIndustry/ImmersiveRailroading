package cam72cam.mod.config;

import org.apache.commons.lang3.StringUtils;

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

    private static Map<Class<?>, Function<Object, String>> encoders = new HashMap<>();
    private static Map<Class<?>, Function<String, Object>> decoders = new HashMap<>();
    private static Map<Class<?>, String> prefixes = new HashMap<>();

    public static <T> void addMapper(Class<T> cls, Function<T, String> encoder, Function<String, T> decoder) {
        encoders.put(cls, x -> encoder.apply((T) x));
        decoders.put(cls, decoder::apply);
        prefixes.put(cls, cls.getSimpleName().substring(0, 1).toUpperCase());
    }

    static {
        addMapper(int.class, i -> (i.toString()), Integer::parseInt);
        addMapper(Integer.class, i -> (i == null ? "" : i.toString()), Integer::parseInt);
        addMapper(long.class, i -> (i.toString()), Long::parseLong);
        addMapper(Long.class, i -> (i == null ? "" : i.toString()), Long::parseLong);
        addMapper(float.class, i -> (i.toString()), Float::parseFloat);
        addMapper(Float.class, i -> (i == null ? "" : i.toString()), Float::parseFloat);
        addMapper(double.class, i -> (i.toString()), Double::parseDouble);
        addMapper(Double.class, i -> (i == null ? "" : i.toString()), Double::parseDouble);

        addMapper(boolean.class, i -> (i.toString()), Boolean::parseBoolean);
        addMapper(Boolean.class, i -> (i == null ? "" : i.toString()), Boolean::parseBoolean);

        addMapper(String.class, i -> (i == null ? "" : i), l -> l);
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

        protected List<String> getFormattedComment() {
            if (getComment().length() == 0) {
                return new ArrayList<>();
            }
            List<String> result = new ArrayList<>();
            String[] parts = getComment().split("\n");
            if (parts.length == 1) {
                result.add("# " + parts[0]);
            } else {
                int max = Arrays.stream(parts).map(String::length).sorted(Comparator.reverseOrder()).findFirst().get();
                max = Math.max(max, getName().length());
                result.add(StringUtils.repeat("#", max + 4));
                result.add("# " + getName() + StringUtils.repeat( " ", max - getName().length()) + " #");
                result.add("# " + StringUtils.repeat( "-", max) + " #");
                for (String part : parts) {
                    result.add("# " + part + StringUtils.repeat( " ", max - part.length()) + " #");
                }
                result.add(StringUtils.repeat("#", max + 4));
            }
            return result;
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
            lines.addAll(getFormattedComment());

            if (field.getType().isArray()) {
                Class aType = field.getType().getComponentType();
                lines.add(getName() + " <");
                try {
                    Object[] data = (Object[]) field.get(null);
                    for (Object elem : data) {
                        lines.add("    " + encoders.get(aType).apply(elem));
                    }
                } catch (IllegalAccessException|NullPointerException e) {
                    throw new RuntimeException("Error writing field " + field, e);
                }
                lines.add(">");
                lines.add("");
                return lines;
            }

            if (Map.class.isAssignableFrom(field.getType())) {
                lines.add(getName() + " {");
                try {
                    Map<Object, Object> data = (Map<Object, Object>) field.get(null);
                    for(Object key : data.keySet()) {
                        Object value = data.get(key);
                        lines.add("    " + prefixes.get(value.getClass()) + ":" + encoders.get(key.getClass()).apply(key) + "=" + encoders.get(value.getClass()).apply(value));
                    }
                } catch (IllegalAccessException|NullPointerException e) {
                    throw new RuntimeException("Error writing field " + field, e);
                }
                lines.add("}");
                lines.add("");
                return lines;
            }


            try {
                lines.add(prefixes.get(field.getType()) + ":" + getName() + "=" + encoders.get(field.getType()).apply(field.get(null)));
            } catch (IllegalAccessException|NullPointerException e) {
                throw new RuntimeException("Error writing field " + field, e);
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
            properties.sort(Comparator.comparing(Property::getName));
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
            lines.addAll(getFormattedComment());
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
