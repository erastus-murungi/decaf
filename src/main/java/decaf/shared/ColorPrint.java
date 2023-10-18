package decaf.shared;

import org.jetbrains.annotations.NotNull;

import static com.google.common.base.Preconditions.checkState;

public class ColorPrint {
    public enum Color {
        BLACK(30),
        RED(31),
        GREEN(32),
        YELLOW(33),
        BLUE(34),
        MAGENTA(35),
        CYAN(36),
        BRIGHT_RED(91),
        WHITE(37);

        private final int colorCode;

        Color(int colorCode) {
            this.colorCode = colorCode;
        }

        public int getColorCode() {
            return colorCode;
        }
    }

    public enum Format {
        NORMAL(0),
        BOLD(1),
        FAINT(2),
        ITALIC(3),
        UNDERLINE(4),
        BLINK(5),
        REVERSE(7),
        HIDDEN(8),
        STRIKETHROUGH(9);

        private final int formatCode;

        Format(int formatCode) {
            this.formatCode = formatCode;
        }

        public int getFormatCode() {
            return formatCode;
        }
    }

    public static String getColoredString24Bit(@NotNull String string, int R, int G, int B) {
        return "\u001B[38;2;" + R + ";" + G + ";" + B + "m" + string + resetColor();
    }


    public static String getColoredString256(@NotNull String string, int id) {
        checkState(id >= 0 && id <= 255, "id must be in range [0, 255]");
        return "\u001B[38;5;" + id + "m" + string + resetColor();
    }

    public static String resetColor() {
        return "\u001B[0m";
    }

    public static String getColoredString(@NotNull String string, int color, int format) {
        return String.format("\u001B[%d;%dm", format, color) + string + resetColor();
    }
    public static String getColoredString(@NotNull String string, @NotNull Color color, @NotNull Format format) {
        return getColoredString(string, color.getColorCode(), format.getFormatCode());
    }

    public static String getColoredString(@NotNull String string, int color) {
        return getColoredString(string, color, Format.NORMAL.getFormatCode());
    }
    public static String getColoredString(@NotNull String string, @NotNull Color color) {
        return getColoredString(string, color.getColorCode(), Format.NORMAL.getFormatCode());
    }

    public static String getColoredStringBg(@NotNull String string, @NotNull Color color) {
        return String.format("\u001B[%dm", color.getColorCode() + 10) + string + resetColor();
    }
}
