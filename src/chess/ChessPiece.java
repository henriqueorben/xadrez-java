package chess;

import boardgame.Board;
import boardgame.Piece;
import boardgame.Position;

public abstract class ChessPiece extends Piece {
    private final Color color;
    private int moveCount;

    public ChessPiece(Board board, Color color) {
        super(board);
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public int getMoveCount() {
        return moveCount;
    }

    void increaseMoveCount() {
        moveCount++;
    }

    protected boolean canMoveTo(Piece occupant) {
        return occupant == null || (occupant instanceof ChessPiece other && other.color != color);
    }

    protected void markSlidingMoves(boolean[][] moves, int rowStep, int columnStep) {
        Board board = getBoard();
        Position target = new Position(position.getRow() + rowStep, position.getColumn() + columnStep);
        while (board.positionExists(target)) {
            Piece occupant = board.piece(target);
            moves[target.getRow()][target.getColumn()] = canMoveTo(occupant);
            if (occupant != null) {
                break;
            }
            target.setRow(target.getRow() + rowStep);
            target.setColumn(target.getColumn() + columnStep);
        }
    }
}
