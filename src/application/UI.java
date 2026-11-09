package application;

import java.util.List;
import java.util.Locale;

import chess.ChessException;
import chess.ChessMatch.DrawReason;
import chess.ChessPiece;
import chess.ChessPosition;
import chess.Color;
import chess.Promotion;

public class UI {
    public static ChessPosition readChessPosition(String input) {
        String square = input.strip();
        if (square.length() != 2) {
            throw new ChessException("Digite uma casa como e2.");
        }
        return new ChessPosition(Character.toLowerCase(square.charAt(0)), square.charAt(1) - '0');
    }

    public static Promotion readPromotion(String input) {
        return switch (input.strip().toUpperCase(Locale.ROOT)) {
            case "", "Q" -> Promotion.QUEEN;
            case "R" -> Promotion.ROOK;
            case "B" -> Promotion.BISHOP;
            case "N" -> Promotion.KNIGHT;
            default -> throw new ChessException("Escolha Q, R, B ou N para a promoção.");
        };
    }

    public static void printBoard(ChessPiece[][] pieces) {
        printBoard(pieces, null);
    }

    public static void printTurn(int turn, Color currentPlayer) {
        String side = currentPlayer == Color.WHITE ? "brancas" : "pretas";
        System.out.println("Turno " + turn + " | Vez das " + side);
    }

    public static void printCheckMate(Color losingPlayer) {
        String winner = losingPlayer == Color.WHITE ? "Pretas" : "Brancas";
        System.out.println("Xeque-mate! " + winner + " venceram.");
    }

    public static void printDraw(DrawReason reason) {
        System.out.println("Empate por " + drawDescription(reason) + ".");
    }

    static String drawDescription(DrawReason reason) {
        return switch (reason) {
            case INSUFFICIENT_MATERIAL -> "material insuficiente";
            case THREEFOLD_REPETITION -> "tripla repetição da posição";
            case FIVEFOLD_REPETITION -> "repetição quíntupla da posição";
            case FIFTY_MOVES -> "regra dos 50 lances sem captura nem movimento de peão";
            case SEVENTY_FIVE_MOVES -> "regra dos 75 lances sem captura nem movimento de peão";
        };
    }

    public static void printCapturedPieces(List<ChessPiece> capturedPieces) {
        System.out.println("Brancas capturadas: " + capturedSymbols(capturedPieces, Color.WHITE));
        System.out.println("Pretas capturadas: " + capturedSymbols(capturedPieces, Color.BLACK));
    }

    private static List<String> capturedSymbols(List<ChessPiece> capturedPieces, Color color) {
        return capturedPieces.stream()
                .filter(piece -> piece.getColor() == color)
                .map(UI::pieceSymbol)
                .toList();
    }

    public static void printBoard(ChessPiece[][] pieces, boolean[][] possibleMoves) {
        for (int row = 0; row < pieces.length; row++) {
            System.out.print((pieces.length - row) + " ");
            for (int column = 0; column < pieces[row].length; column++) {
                String symbol = pieceSymbol(pieces[row][column]);
                boolean highlighted = possibleMoves != null && possibleMoves[row][column];
                System.out.print(highlighted ? "[" + symbol + "] " : " " + symbol + "  ");
            }
            System.out.println();
        }
        System.out.println("   a   b   c   d   e   f   g   h");
    }

    private static String pieceSymbol(ChessPiece piece) {
        if (piece == null) {
            return "-";
        }
        String symbol = piece.toString();
        return piece.getColor() == Color.BLACK ? symbol.toLowerCase(Locale.ROOT) : symbol;
    }
}
