package chess.pieces;

import boardgame.Board;
import chess.ChessPiece;
import chess.Color;

public class Queen extends ChessPiece {
    public Queen(Board board, Color color) {
        super(board, color);
    }

    @Override
    protected boolean[][] calculatePossibleMoves() {
        Board board = getBoard();
        boolean[][] moves = new boolean[board.getRows()][board.getColumns()];
        markSlidingMoves(moves, -1, -1);
        markSlidingMoves(moves, -1, 0);
        markSlidingMoves(moves, -1, 1);
        markSlidingMoves(moves, 0, -1);
        markSlidingMoves(moves, 0, 1);
        markSlidingMoves(moves, 1, -1);
        markSlidingMoves(moves, 1, 0);
        markSlidingMoves(moves, 1, 1);
        return moves;
    }

    @Override
    public String toString() {
        return "Q";
    }
}
