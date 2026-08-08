package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.registry.DefinitionManager;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * A simple compiler for augment stock filtering logics, supports && and || and parens.
 * <p>
 * The compiler is case-sensitive.
 */
public class StockFilterCompiler {
    private static final Map<String, BiFunction<EntityRollingStock, String, Boolean>> PREFIXES = new HashMap<>();
    private static final Map<String, Integer> PRECEDENCE = new HashMap<>();

    private static final String AND = "&&";
    private static final String OR = "||";
    private static final String START_PAREN = "(";
    private static final String END_PAREN = ")";

    static {
        // type:[stock type]
        PREFIXES.put("type", (stock, content) ->
                switch (content) {
                    case "locomotive" -> stock instanceof LocomotiveDiesel
                            || stock instanceof LocomotiveSteam
                            || stock instanceof HandCar;
                    case "diesel" -> stock instanceof LocomotiveDiesel;
                    case "steam" -> stock instanceof LocomotiveSteam;
                    case "handcar" -> stock instanceof HandCar;
                    case "passenger" -> stock instanceof CarPassenger;
                    case "tender" -> stock instanceof Tender;
                    case "tank" -> stock instanceof CarTank;
                    case "freight" -> stock instanceof CarFreight;
                    default -> false;
                });
        // tag:[stock tag] (tags defined in json/caml)
        PREFIXES.put("tag", (stock, content) -> DefinitionManager.isTaggedWith(stock.getDefinition(), content));
        // stock:[definition name]
        PREFIXES.put("stock", (stock, content) -> {
            String definitionFileName = stock.getDefinitionID().split("/")[2];
            return definitionFileName.substring(0, definitionFileName.length() - 5).equals(content);
        });
        // works:[locomotive works]
        PREFIXES.put("works", (stock, content) -> stock instanceof Locomotive && ((Locomotive) stock).getDefinition().works.equals(content));
        // author:[modeler name]
        PREFIXES.put("author", (stock, content) -> stock.getDefinition().modelerName.equals(content));
        // pack:[pack name]
        PREFIXES.put("pack", (stock, content) -> stock.getDefinition().packName.equals(content));
        // nametag:[nametagged stock name]
        PREFIXES.put("nametag", (stock, content) -> content.equals(stock.tag));

        // Lower value = lower precedence
        PRECEDENCE.put(OR, 1);
        PRECEDENCE.put(AND, 2);

    }

    /** Compile a filter expression into a predicate */
    public static Predicate<EntityRollingStock> compile(String expression, boolean defaultVal) {
        if (expression == null || expression.trim().isEmpty()) {
            return _ -> defaultVal;
        }

        List<String> tokens = tokenize(expression);
        List<String> rpn = toRPN(tokens);
        return evaluate(rpn);
    }

    /** Splits an expression into operand/operator/parens, preserving original semantics. */
    private static List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        int parenDepth = 0;

        Runnable flushBuffer = () -> {
            if (!buffer.isEmpty()) {
                String token = buffer.toString().trim();
                if (!token.isEmpty()) {
                    tokens.add(token);
                }
                buffer.setLength(0);
            }
        };

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            switch (c) {
                case '(':
                    flushBuffer.run();
                    parenDepth++;
                    tokens.add(START_PAREN);
                    break;
                case ')':
                    flushBuffer.run();
                    parenDepth--;
                    if (parenDepth < 0) {
                        throw new IllegalArgumentException("Unmatched closing parenthesis");
                    }
                    tokens.add(END_PAREN);
                    break;
                case '&':
                    if (i + 1 < expression.length() && expression.charAt(i + 1) == '&') {
                        flushBuffer.run();
                        tokens.add(AND);
                        i++;
                    } else {
                        buffer.append(c);
                    }
                    break;
                case '|':
                    if (i + 1 < expression.length() && expression.charAt(i + 1) == '|') {
                        flushBuffer.run();
                        tokens.add(OR);
                        i++;
                    } else {
                        buffer.append(c);
                    }
                    break;
                default:
                    buffer.append(c);
            }
        }

        if (parenDepth != 0) {
            throw new IllegalArgumentException("Unmatched opening parenthesis");
        }

        flushBuffer.run();
        return tokens;
    }

    /** Converts an infix token list into Reverse Polish Notation using the shunting-yard algorithm */
    private static List<String> toRPN(List<String> tokens) {
        List<String> output = new ArrayList<>(tokens.size());
        Deque<String> operators = new ArrayDeque<>();

        for (String token : tokens) {
            switch (token) {
                case START_PAREN -> operators.push(token);
                case END_PAREN -> {
                    while (!operators.isEmpty() && !operators.peek().equals(START_PAREN)) {
                        output.add(operators.pop());
                    }
                    if (operators.isEmpty()) {
                        throw new IllegalArgumentException("Unmatched closing parenthesis");
                    }
                    // Discard the matching '('
                    operators.pop();
                }
                case AND, OR -> {
                    // Pop operators with equal or higher precedence onto the output first.
                    while (!operators.isEmpty()
                            && PRECEDENCE.containsKey(operators.peek())
                            && PRECEDENCE.get(operators.peek()) >= PRECEDENCE.get(token)) {
                        output.add(operators.pop());
                    }
                    operators.push(token);
                }
                // Operand
                default -> output.add(token);
            }
        }

        while (!operators.isEmpty()) {
            String op = operators.pop();
            if (op.equals(START_PAREN)) {
                throw new IllegalArgumentException("Unmatched opening parenthesis");
            }
            output.add(op);
        }

        return output;
    }

    /** Evaluates a Reverse Polish Notation token list into a single predicate. */
    private static Predicate<EntityRollingStock> evaluate(List<String> rpn) {
        Deque<Predicate<EntityRollingStock>> stack = new ArrayDeque<>();

        for (String token : rpn) {
            switch (token) {
                case AND, OR:
                    combine(stack, token);
                    break;
                default:
                    stack.push(compileToken(token));
            }
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException("Invalid filter expression");
        }
        return stack.pop();
    }

    /** Pops two predicates from the stack and pushes back their operated combination. */
    private static void combine(Deque<Predicate<EntityRollingStock>> stack, String operator) {
        if (stack.size() < 2) {
            throw new IllegalArgumentException("Missing operand for " + operator);
        }
        Predicate<EntityRollingStock> right = stack.pop();
        Predicate<EntityRollingStock> left = stack.pop();
        if (operator.equals(AND)) {
            stack.push(stock -> left.test(stock) && right.test(stock));
        } else {
            stack.push(stock -> left.test(stock) || right.test(stock));
        }
    }

    /** Builds a predicate for a single KV pair */
    private static Predicate<EntityRollingStock> compileToken(String token) {
        String[] parts = token.split(":", 2);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            throw new IllegalArgumentException("Invalid filter token \"" + token + "\": expected <prefix>:<value>");
        }
        parts[0] = parts[0].trim();
        parts[1] = parts[1].trim();

        BiFunction<EntityRollingStock, String, Boolean> handler = PREFIXES.get(parts[0]);
        if (handler == null) {
            throw new IllegalArgumentException("Unknown filter prefix \"" + parts[0] + "\" in token \"" + token + "\"");
        }

        return stock -> handler.apply(stock, parts[1]);
    }
}
