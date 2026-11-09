package chess.pieces;

import boardgame.Board;
import boardgame.Position;
import chess.ChessPiece;
import chess.Color;

public class King extends ChessPiece {
    public King(Board board, Color color) {
        super(board, color);
    }

    @Override
    protected boolean[][] calculatePossibleMoves() {
        Board board = getBoard();
        boolean[][] moves = new boolean[board.getRows()][board.getColumns()];
        for (int row = position.getRow() - 1; row <= position.getRow() + 1; row++) {
            for (int column = position.getColumn() - 1; column <= position.getColumn() + 1; column++) {
                Position target = new Position(row, column);
                if (board.positionExists(target)) {
                    moves[row][column] = canMoveTo(board.piece(target));
                }
            }
        }
        return moves;
    }

    @Override
    public String toString() {
        return "K";
    }
}
