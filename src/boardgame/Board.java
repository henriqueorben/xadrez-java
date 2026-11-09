package boardgame;

public class Board {
    private final int rows;
    private final int columns;
    private final Piece[][] pieces;

    public Board(int rows, int columns) {
        if (rows < 1 || columns < 1) {
            throw new BoardException("The board must have at least one row and one column.");
        }
        this.rows = rows;
        this.columns = columns;
        this.pieces = new Piece[rows][columns];
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public Piece piece(int row, int column) {
        if (!positionExists(row, column)) {
            throw new BoardException("Position outside the board.");
        }
        return pieces[row][column];
    }

    public Piece piece(Position position) {
        if (position == null) {
            throw new BoardException("A position is required.");
        }
        return piece(position.getRow(), position.getColumn());
    }

    public void placePiece(Piece piece, Position position) {
        if (thereIsAPiece(position)) {
            throw new BoardException("This square is already occupied.");
        }
        if (piece == null || piece.getBoard() != this || piece.position != null) {
            throw new BoardException("The piece must belong to this board and be unplaced.");
        }
        int row = position.getRow();
        int column = position.getColumn();
        pieces[row][column] = piece;
        piece.position = new Position(row, column);
    }

    public Piece removePiece(Position position) {
        Piece piece = piece(position);
        if (piece != null) {
            pieces[position.getRow()][position.getColumn()] = null;
            piece.position = null;
        }
        return piece;
    }

    public boolean positionExists(Position position) {
        return position != null && positionExists(position.getRow(), position.getColumn());
    }

    public boolean thereIsAPiece(Position position) {
        return piece(position) != null;
    }

    private boolean positionExists(int row, int column) {
        return row >= 0 && row < rows && column >= 0 && column < columns;
    }
}
