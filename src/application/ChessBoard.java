package application;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JPanel;

import chess.ChessPiece;
import chess.ChessPosition;

final class ChessBoard extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color LIGHT = new Color(0xE8DBBA);
    private static final Color DARK = new Color(0x769274);
    private static final Color GOLD = new Color(0xF8DA85);
    private static final Font COORDINATES = new Font("SansSerif", Font.BOLD, 12);
    private static final Font PIECES = pieceFont();
    private final SquareButton[][] buttons = new SquareButton[8][8];

    record Square(int row, int column) {
        ChessPosition position() {
            return new ChessPosition((char) ('a' + column), 8 - row);
        }
    }

    ChessBoard(Consumer<Square> onSelect) {
        super(new GridLayout(8, 8));
        setPreferredSize(new Dimension(544, 544));
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                Square square = new Square(row, column);
                SquareButton button = new SquareButton(square);
                button.setName(square.position().toString());
                button.addActionListener(event -> onSelect.accept(square));
                buttons[row][column] = button;
                add(button);
            }
        }
    }

    void showPosition(ChessPiece[][] pieces, Square selection, boolean[][] moves, boolean finished) {
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                SquareButton button = buttons[row][column];
                button.update(pieces[row][column], button.square.equals(selection),
                        moves != null && moves[row][column], finished);
            }
        }
    }

    static Font symbolFont(float size) {
        return PIECES.deriveFont(size);
    }

    static char symbol(ChessPiece piece, boolean outlinedWhite) {
        int offset = switch (piece.toString()) {
            case "K" -> 0;
            case "Q" -> 1;
            case "R" -> 2;
            case "B" -> 3;
            case "N" -> 4;
            case "P" -> 5;
            default -> throw new IllegalArgumentException("Peça desconhecida.");
        };
        boolean white = piece.getColor() == chess.Color.WHITE;
        return (char) ((outlinedWhite && white ? '\u2654' : '\u265A') + offset);
    }

    private static Font pieceFont() {
        for (String name : new String[]{"Segoe UI Symbol", "DejaVu Sans", "Dialog"}) {
            Font font = new Font(name, Font.PLAIN, 24);
            if (font.canDisplayUpTo("♔♕♖♗♘♙♚♛♜♝♞♟") == -1) {
                return font;
            }
        }
        return new Font("Dialog", Font.PLAIN, 24);
    }

    private static String description(ChessPiece piece) {
        String type = switch (piece.toString()) {
            case "K" -> "rei";
            case "Q" -> "dama";
            case "R" -> "torre";
            case "B" -> "bispo";
            case "N" -> "cavalo";
            case "P" -> "peão";
            default -> "peça";
        };
        return type + " das " + (piece.getColor() == chess.Color.WHITE ? "brancas" : "pretas");
    }

    @SuppressWarnings("serial")
    private static final class SquareButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final Square square;
        private final boolean light;
        private ChessPiece piece;
        private boolean selected;
        private boolean destination;

        private SquareButton(Square square) {
            this.square = square;
            this.light = (square.row() + square.column()) % 2 == 0;
            setBorderPainted(false);
            setContentAreaFilled(false);
            setRolloverEnabled(true);
        }

        private void update(ChessPiece piece, boolean selected, boolean destination, boolean finished) {
            this.piece = piece;
            this.selected = selected;
            this.destination = destination;
            String label = square.position() + ", " + (piece == null ? "casa vazia" : description(piece));
            if (destination) {
                label += ", destino disponível";
            } else if (selected) {
                label += ", selecionada";
            }
            getAccessibleContext().setAccessibleName(label);
            setToolTipText(label);
            setEnabled(!finished);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            int size = Math.min(width, height);
            Color base = light ? LIGHT : DARK;
            g.setColor(selected ? GOLD : base);
            g.fillRect(0, 0, width, height);
            if (isEnabled() && getModel().isRollover()) {
                g.setColor(new Color(255, 255, 255, 35));
                g.fillRect(0, 0, width, height);
            }
            if (destination) {
                g.setColor(new Color(0x355542));
                if (piece == null) {
                    int dot = Math.max(6, size / 4);
                    g.fillOval((width - dot) / 2, (height - dot) / 2, dot, dot);
                } else {
                    g.setStroke(new BasicStroke(4));
                    g.drawOval(5, 5, width - 11, height - 11);
                }
            }
            if (piece != null) {
                drawPiece(g, width, height, size);
            }
            g.setFont(COORDINATES);
            g.setColor(light || selected ? new Color(0x365344) : new Color(0xF5E8CB));
            if (square.column() == 0) {
                g.drawString(Integer.toString(8 - square.row()), 5, 14);
            }
            if (square.row() == 7) {
                String file = Character.toString('a' + square.column());
                FontMetrics metrics = g.getFontMetrics();
                g.drawString(file, width - metrics.stringWidth(file) - 5, height - 5);
            }
            if (isFocusOwner()) {
                g.setColor(new Color(0x265448));
                g.setStroke(new BasicStroke(2));
                g.drawRect(2, 2, width - 5, height - 5);
            }
            g.dispose();
        }

        private void drawPiece(Graphics2D g, int width, int height, int size) {
            String icon = Character.toString(symbol(piece, false));
            Font font = symbolFont(size * 0.74f);
            if (!font.canDisplay(icon.charAt(0))) {
                font = COORDINATES.deriveFont(size * 0.55f);
                icon = piece.toString();
            }
            Shape glyph = font.createGlyphVector(g.getFontRenderContext(), icon).getOutline();
            Rectangle2D bounds = glyph.getBounds2D();
            Shape centered = AffineTransform.getTranslateInstance(
                    width / 2.0 - bounds.getCenterX(), height / 2.0 - bounds.getCenterY())
                    .createTransformedShape(glyph);
            boolean white = piece.getColor() == chess.Color.WHITE;
            g.setColor(white ? new Color(0x293631) : new Color(0xCBD3B5));
            g.setStroke(new BasicStroke(white ? 2.5f : 1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(centered);
            g.setColor(white ? new Color(0xFFF7E5) : new Color(0x1B2D27));
            g.fill(centered);
        }
    }
}
