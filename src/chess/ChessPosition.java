package chess;

import boardgame.Position;

public class ChessPosition {
    private final char column;
    private final int row;

    public ChessPosition(char column, int row) {
        if (column < 'a' || column > 'h' || row < 1 || row > 8) {
            throw new ChessException("Use uma casa válida entre a1 e h8.");
        }
        this.column = column;
        this.row = row;
    }

    Position toPosition() {
        return new Position(8 - row, column - 'a');
    }

    @Override
    public String toString() {
        return Character.toString(column) + row;
    }
}
