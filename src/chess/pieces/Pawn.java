package chess.pieces;

import boardgame.Board;
import boardgame.Piece;
import boardgame.Position;
import chess.ChessPiece;
import chess.Color;

public class Pawn extends ChessPiece {
    public Pawn(Board board, Color color) {
        super(board, color);
    }

    @Override
    protected boolean[][] calculatePossibleMoves() {
        Board board = getBoard();
        boolean[][] moves = new boolean[board.getRows()][board.getColumns()];
        int row = position.getRow();
        int column = position.getColumn();
        int direction = getColor() == Color.WHITE ? -1 : 1;
        int startingRow = getColor() == Color.WHITE ? board.getRows() - 2 : 1;
        Position forward = new Position(row + direction, column);

        if (board.positionExists(forward) && !board.thereIsAPiece(forward)) {
            moves[forward.getRow()][column] = true;
            Position doubleStep = new Position(row + 2 * direction, column);
            if (row == startingRow && getMoveCount() == 0
                    && board.positionExists(doubleStep) && !board.thereIsAPiece(doubleStep)) {
                moves[doubleStep.getRow()][column] = true;
            }
        }

        markCapture(moves, new Position(forward.getRow(), column - 1));
        markCapture(moves, new Position(forward.getRow(), column + 1));
        return moves;
    }

    private void markCapture(boolean[][] moves, Position target) {
        Board board = getBoard();
        if (board.positionExists(target)) {
            Piece occupant = board.piece(target);
            moves[target.getRow()][target.getColumn()] = occupant != null && canMoveTo(occupant);
        }
    }

    @Override
    public String toString() {
        return "P";
    }
}
