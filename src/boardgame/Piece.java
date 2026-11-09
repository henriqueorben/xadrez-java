package boardgame;

public abstract class Piece {
    protected Position position;
    private final Board board;

    public Piece(Board board) {
        this.board = board;
    }

    protected Board getBoard() {
        return board;
    }

    protected abstract boolean[][] calculatePossibleMoves();

    public final boolean[][] possibleMoves() {
        if (position == null) {
            return new boolean[board.getRows()][board.getColumns()];
        }
        return calculatePossibleMoves();
    }

    public boolean possibleMove(Position target) {
        return position != null && board.positionExists(target)
                && calculatePossibleMoves()[target.getRow()][target.getColumn()];
    }
}
