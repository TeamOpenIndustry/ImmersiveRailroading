package cam72cam.mod.config;

import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConfigFile {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Name {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Comment {
        String value();
    }

    public static void sync(Class cls, Path path) {
        PropertyClass pc = new PropertyClass(cls);

        if (Files.exists(path)) {
            List<String> lines;
            try {
                lines = Files.readAllLines(path);
            } catch (Exception e) {
                throw new RuntimeException("Unable to read config file " + path, e);
            }

            lines = lines.stream()
                    .filter(x -> !x.matches("\\s*#.*"))
                    .filter(x -> !x.matches("\\s*"))
                    .map(String::trim)
                    .collect(Collectors.toList());

            if (lines.size() > 2) {
                pc.read(lines);
            }
        }
        try {
            Files.write(path, String.join(System.lineSeparator(), pc.write()).getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Unable to write config file " + path, e);
        }
    }

    private static Map<Class<?>, Function<Object, String>> encoders = new HashMap<>();
    private static Map<Class<?>, Function<String, Object>> decoders = new HashMap<>();

    public static <T> void addMapper(Class<T> cls, Function<T, String> encoder, Function<String, T> decoder) {
        encoders.put(cls, x -> encoder.apply((T) x));
        decoders.put(cls, decoder::apply);
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

    private static String getPrefix(Class<?> cls) {
        if (cls.isEnum()) {
            return "S";
        }
        return cls.getSimpleName().substring(0, 1).toUpperCase();
    }

    private static String encode(Class<?> cls, Object o) {
        if (cls.isEnum()) {
            return ((Enum) o).name();
        }
        return encoders.get(cls).apply(o);
    }

    private static Object decode(Class<?> cls, String s) {
        if (cls.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>)cls, s);
        }
        return decoders.get(cls).apply(s);
    }

    private abstract static class Property {
        protected abstract <A extends Annotation> A getAnnotation(Class<A> cls);

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
        protected void read(List<String> lines) {
            if (field.getType().isArray()) {
                lines.remove(0);
                List<String> found = new ArrayList<>();
                while(!lines.isEmpty()) {
                    String line = lines.remove(0);
                    if(line.equals(">")) {
                        try {
                            Object[] array = (Object[]) Array.newInstance(field.getType().getComponentType(), found.size());
                            for (int i = 0; i < found.size(); i++) {
                                array[i] = decode(field.getType().getComponentType(), found.get(i));
                            }
                            field.set(null, array);
                        } catch (IllegalAccessException|NullPointerException e) {
                            System.out.println("Error reading field " + field);
                            e.printStackTrace();
                        }
                        return;
                    } else {
                        found.add(line);
                    }
                }
            }

            if (Map.class.isAssignableFrom(field.getType())) {
                lines.remove(0);
                List<String> found = new ArrayList<>();
                while(!lines.isEmpty()) {
                    String line = lines.remove(0);
                    if (line.equals("}")) {

                        try {
                            Map<Object, Object> data = (Map<Object, Object>) field.get(null);
                            data.clear();

                            Type[] types = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
                            Class<?> kt = (Class<?>) types[0];
                            Class<?> vt = (Class<?>) types[1];
                            for (String s : found) {
                                String[] sp = s.split("=", 2);
                                Object key = decode(kt, sp[0].substring(2));
                                Object val = decode(vt, sp[1]);
                                data.put(key, val);
                            }
                        } catch (IllegalAccessException|NullPointerException e) {
                            System.out.println("Error reading field " + field);
                            e.printStackTrace();
                        }
                        return;
                    } else {
                        found.add(line);
                    }
                }
            }

            String line = lines.remove(0);
            try {
                field.set(null, decode(field.getType(), line.split("=", 2)[1]));
            } catch (IllegalAccessException|NullPointerException e) {
                System.out.println("Error reading field " + field);
                e.printStackTrace();
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
                        lines.add("    " + encode(aType, elem));
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
                        lines.add("    " + getPrefix(value.getClass()) + ":" + encode(key.getClass(), key) + "=" + encode(value.getClass(), value));
                    }
                } catch (IllegalAccessException|NullPointerException e) {
                    throw new RuntimeException("Error writing field " + field, e);
                }
                lines.add("}");
                lines.add("");
                return lines;
            }


            try {
                lines.add(getPrefix(field.getType()) + ":" + getName() + "=" + encode(field.getType(), field.get(null)));
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
        protected void read(List<String> lines) {
            lines.remove(0);
            while(!lines.isEmpty()) {
                String line = lines.get(0);

                if (line.equals("}")) {
                    lines.remove(0);
                    return;
                }

                String[] parts = line.split("[ =]");
                Property prop = properties.stream().filter(x -> x.getName().equals(parts[0]) || x.getName().equals(parts[0].substring(2))).findFirst().orElse(null);
                if (prop != null) {
                    prop.read(lines);
                } else {
                    lines.remove(0);
                }
            }
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
