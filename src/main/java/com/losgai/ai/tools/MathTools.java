package com.losgai.ai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class MathTools {

    @Tool(name = "Calculate", description = "Perform basic arithmetic calculations: add, subtract, multiply, divide. Returns the result as a number.")
    String calculate(
            @ToolParam(description = "First operand") double a,
            @ToolParam(description = "Operator: +, -, *, /") String operator,
            @ToolParam(description = "Second operand") double b) {
        return switch (operator.trim()) {
            case "+" -> String.valueOf(a + b);
            case "-" -> String.valueOf(a - b);
            case "*" -> String.valueOf(a * b);
            case "/" -> {
                if (b == 0) yield "Error: Division by zero";
                yield String.valueOf(a / b);
            }
            default -> "Error: Unsupported operator '" + operator + "'. Use +, -, *, /";
        };
    }

    @Tool(name = "SquareRoot", description = "Calculate the square root of a non-negative number")
    String squareRoot(
            @ToolParam(description = "The number to calculate square root for") double number) {
        if (number < 0) {
            return "Error: Cannot calculate square root of a negative number";
        }
        return String.valueOf(Math.sqrt(number));
    }

    @Tool(name = "Power", description = "Calculate base raised to the power of exponent")
    String power(
            @ToolParam(description = "The base number") double base,
            @ToolParam(description = "The exponent") double exponent) {
        return String.valueOf(Math.pow(base, exponent));
    }

    @Tool(name = "RoundNumber", description = "Round a number to specified decimal places")
    String roundNumber(
            @ToolParam(description = "The number to round") double number,
            @ToolParam(description = "Number of decimal places") int decimalPlaces) {
        if (decimalPlaces < 0) {
            return "Error: Decimal places cannot be negative";
        }
        double factor = Math.pow(10, decimalPlaces);
        return String.valueOf(Math.round(number * factor) / factor);
    }

    @Tool(name = "Percentage", description = "Calculate percentage: what percent is 'part' of 'total', or calculate 'percent' of 'total'")
    String percentage(
            @ToolParam(description = "Operation type: 'of' to calculate percent of total, 'what_percent' to find what percent part is of total") String operation,
            @ToolParam(description = "The percent value (used when operation is 'of') or the part value (used when operation is 'what_percent')") double value,
            @ToolParam(description = "The total value") double total) {
        if (total == 0) {
            return "Error: Total cannot be zero";
        }
        return switch (operation.trim()) {
            case "of" -> String.valueOf(value / 100.0 * total);
            case "what_percent" -> String.valueOf(value / total * 100.0);
            default -> "Error: Unknown operation '" + operation + "'. Use 'of' or 'what_percent'";
        };
    }
}
