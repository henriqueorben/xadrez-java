package chess.pieces;

import boardgame.Board;
import boardgame.Position;
import chess.ChessPiece;
import chess.Color;

public class Knight extends ChessPiece {
    private static final int[][] OFFSETS = {
        {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
        {1, -2}, {1, 2}, {2, -1}, {2, 1}
    };

    public Knight(Board board, Color color) {
        super(board, color);
    }

    @Override
    protected boolean[][] calculatePossibleMoves() {
        Board board = getBoard();
        boolean[][] moves = new boolean[board.getRows()][board.getColumns()];
        for (int[] offset : OFFSETS) {
            Position target = new Position(position.getRow() + offset[0], position.getColumn() + offset[1]);
            if (board.positionExists(target)) {
                moves[target.getRow()][target.getColumn()] = canMoveTo(board.piece(target));
            }
        }
        return moves;
    }

    @Override
    public String toString() {
        return "N";
    }
}
