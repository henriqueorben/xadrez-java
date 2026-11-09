package chess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import boardgame.Board;
import boardgame.Position;
import chess.pieces.Bishop;
import chess.pieces.King;
import chess.pieces.Knight;
import chess.pieces.Pawn;
import chess.pieces.Queen;
import chess.pieces.Rook;

public class ChessMatch {
    public enum DrawReason {
        INSUFFICIENT_MATERIAL,
        THREEFOLD_REPETITION,
        FIVEFOLD_REPETITION,
        FIFTY_MOVES,
        SEVENTY_FIVE_MOVES
    }

    private final Board board = new Board(8, 8);
    private final List<ChessPiece> capturedPieces = new ArrayList<>();
    private final Map<String, Integer> positionHistory = new HashMap<>();
    private int turn = 1;
    private Color currentPlayer = Color.WHITE;
    private Position enPassantTarget;
    private int quietHalfMoves;
    private int positionOccurrences;
    private DrawReason claimedDraw;

    private record Move(Position source, Position target, Position captureSquare,
                        Position rookSource, Position rookTarget) {
        private Move(Position source, Position target) {
            this(source, target, target, null, null);
        }
    }

    public ChessMatch() {
        initialSetup();
        recordPosition();
    }

    public int getTurn() {
        return turn;
    }

    public Color getCurrentPlayer() {
        return currentPlayer;
    }

    public List<ChessPiece> getCapturedPieces() {
        return List.copyOf(capturedPieces);
    }

    public boolean isInCheck() {
        return isInCheck(currentPlayer);
    }

    public boolean isCheckMate() {
        return isInCheck() && !hasLegalMove();
    }

    public boolean isStalemate() {
        return !isInCheck() && !hasLegalMove();
    }

    public DrawReason automaticDraw() {
        if (hasInsufficientMaterial()) {
            return DrawReason.INSUFFICIENT_MATERIAL;
        }
        if (positionOccurrences >= 5) {
            return DrawReason.FIVEFOLD_REPETITION;
        }
        if (quietHalfMoves >= 150 && !isCheckMate()) {
            return DrawReason.SEVENTY_FIVE_MOVES;
        }
        return null;
    }

    public DrawReason drawReason() {
        return claimedDraw != null ? claimedDraw : automaticDraw();
    }

    public void claimDraw() {
        requireClaimableGame();
        acceptDrawClaim(positionOccurrences, quietHalfMoves);
    }

    public void claimDraw(ChessPosition source, ChessPosition target) {
        requireClaimableGame();
        Move move = validateMove(source, target);
        if (board.piece(move.source()) instanceof Pawn || board.piece(move.captureSquare()) != null) {
            throw new ChessException("Essa jogada não completa uma condição de empate.");
        }

        // A jogada anunciada só é simulada. Um pedido aceito encerra a partida na posição atual.
        makeMove(move);
        try {
            int occurrences = positionHistory.getOrDefault(positionKey(opponent(), null), 0);
            acceptDrawClaim(occurrences + 1, quietHalfMoves + 1);
        } finally {
            undoMove(move, null);
        }
    }

    private void requireClaimableGame() {
        if (drawReason() != null || !hasLegalMove()) {
            throw new ChessException("A partida já terminou.");
        }
    }

    private void acceptDrawClaim(int occurrences, int halfMoves) {
        if (occurrences >= 3) {
            claimedDraw = DrawReason.THREEFOLD_REPETITION;
        } else if (halfMoves >= 100) {
            claimedDraw = DrawReason.FIFTY_MOVES;
        } else {
            throw new ChessException("O pedido ainda não atende à regra da tripla repetição nem à dos 50 lances.");
        }
    }

    public ChessPiece[][] getPieces() {
        ChessPiece[][] pieces = new ChessPiece[board.getRows()][board.getColumns()];
        for (int row = 0; row < board.getRows(); row++) {
            for (int column = 0; column < board.getColumns(); column++) {
                pieces[row][column] = (ChessPiece) board.piece(row, column);
            }
        }
        return pieces;
    }

    public boolean[][] possibleMoves(ChessPosition sourcePosition) {
        if (sourcePosition == null) {
            throw new ChessException("Informe a casa de origem.");
        }
        Position source = sourcePosition.toPosition();
        return legalMoves(source, requireCurrentPlayerPieceAt(source));
    }

    private boolean[][] legalMoves(Position source, ChessPiece piece) {
        boolean[][] moves = candidateMoves(source, piece);
        for (int row = 0; row < moves.length; row++) {
            for (int column = 0; column < moves[row].length; column++) {
                if (moves[row][column]) {
                    Position target = new Position(row, column);
                    moves[row][column] = !(board.piece(target) instanceof King)
                            && !wouldLeaveKingInCheck(describeMove(source, target, piece));
                }
            }
        }
        return moves;
    }

    public ChessPiece performChessMove(ChessPosition sourcePosition, ChessPosition targetPosition) {
        return performChessMove(sourcePosition, targetPosition, Promotion.QUEEN);
    }

    public ChessPiece performChessMove(ChessPosition sourcePosition, ChessPosition targetPosition,
                                       Promotion promotion) {
        if (drawReason() != null) {
            throw new ChessException("A partida terminou em empate.");
        }
        Move move = validateMove(sourcePosition, targetPosition);
        Position source = move.source();
        Position target = move.target();
        ChessPiece movingPiece = (ChessPiece) board.piece(source);
        ChessPiece promotedPiece = promotes(movingPiece, target)
                ? promotedPiece(movingPiece.getColor(), promotion == null ? Promotion.QUEEN : promotion)
                : null;
        ChessPiece capturedPiece = makeMove(move);
        movingPiece.increaseMoveCount();
        if (move.rookTarget() != null) {
            ((ChessPiece) board.piece(move.rookTarget())).increaseMoveCount();
        }
        if (capturedPiece != null) {
            capturedPieces.add(capturedPiece);
        }
        if (promotedPiece != null) {
            board.removePiece(target);
            board.placePiece(promotedPiece, target);
        }
        enPassantTarget = null;
        if (movingPiece instanceof Pawn && Math.abs(source.getRow() - target.getRow()) == 2) {
            enPassantTarget = new Position((source.getRow() + target.getRow()) / 2, source.getColumn());
        }
        quietHalfMoves = movingPiece instanceof Pawn || capturedPiece != null ? 0 : quietHalfMoves + 1;
        turn++;
        currentPlayer = opponent();
        recordPosition();
        return capturedPiece;
    }

    private Move validateMove(ChessPosition sourcePosition, ChessPosition targetPosition) {
        if (sourcePosition == null || targetPosition == null) {
            throw new ChessException("Informe as casas de origem e destino.");
        }
        Position source = sourcePosition.toPosition();
        Position target = targetPosition.toPosition();
        ChessPiece movingPiece = requireCurrentPlayerPieceAt(source);

        ChessPiece targetPiece = (ChessPiece) board.piece(target);
        if (targetPiece != null && targetPiece.getColor() == movingPiece.getColor()) {
            throw new ChessException("A casa de destino já tem uma peça da mesma cor.");
        }
        if (!candidateMoves(source, movingPiece)[target.getRow()][target.getColumn()]) {
            throw new ChessException("Essa peça não pode se mover para a casa de destino.");
        }
        if (targetPiece instanceof King) {
            throw new ChessException("O rei adversário não pode ser capturado.");
        }
        Move move = describeMove(source, target, movingPiece);
        if (wouldLeaveKingInCheck(move)) {
            throw new ChessException("Essa jogada deixaria seu rei em xeque.");
        }
        return move;
    }

    private Color opponent() {
        return currentPlayer == Color.WHITE ? Color.BLACK : Color.WHITE;
    }

    private void recordPosition() {
        Position captureTarget = hasLegalEnPassantCapture() ? enPassantTarget : null;
        positionOccurrences = positionHistory.merge(positionKey(currentPlayer, captureTarget), 1, Integer::sum);
    }

    private String positionKey(Color player, Position captureTarget) {
        StringBuilder key = new StringBuilder(72);
        for (int row = 0; row < board.getRows(); row++) {
            for (int column = 0; column < board.getColumns(); column++) {
                ChessPiece piece = (ChessPiece) board.piece(row, column);
                if (piece == null) {
                    key.append('-');
                } else {
                    char symbol = piece.toString().charAt(0);
                    key.append(piece.getColor() == Color.WHITE ? symbol : Character.toLowerCase(symbol));
                }
            }
        }
        key.append(player == Color.WHITE ? 'w' : 'b');
        for (Color color : Color.values()) {
            int row = color == Color.WHITE ? 7 : 0;
            boolean unmovedKing = board.piece(row, 4) instanceof King king
                    && king.getColor() == color && king.getMoveCount() == 0;
            key.append(unmovedKing && hasUnmovedRook(row, 7, color) ? '1' : '0');
            key.append(unmovedKing && hasUnmovedRook(row, 0, color) ? '1' : '0');
        }
        if (captureTarget != null) {
            key.append(captureTarget.getColumn());
        }
        return key.toString();
    }

    private boolean hasLegalEnPassantCapture() {
        if (enPassantTarget == null) {
            return false;
        }
        int pawnRow = enPassantTarget.getRow() + (currentPlayer == Color.WHITE ? 1 : -1);
        int targetColumn = enPassantTarget.getColumn();
        for (int column = targetColumn - 1; column <= targetColumn + 1; column += 2) {
            Position source = new Position(pawnRow, column);
            if (board.positionExists(source) && board.piece(source) instanceof Pawn pawn
                    && pawn.getColor() == currentPlayer
                    && !wouldLeaveKingInCheck(describeMove(source, enPassantTarget, pawn))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasInsufficientMaterial() {
        int knights = 0;
        int bishopSquareColor = -1;
        for (int row = 0; row < board.getRows(); row++) {
            for (int column = 0; column < board.getColumns(); column++) {
                ChessPiece piece = (ChessPiece) board.piece(row, column);
                if (piece == null || piece instanceof King) {
                    continue;
                }
                if (piece instanceof Knight) {
                    knights++;
                } else if (piece instanceof Bishop) {
                    int squareColor = (row + column) % 2;
                    if (bishopSquareColor != -1 && bishopSquareColor != squareColor) {
                        return false;
                    }
                    bishopSquareColor = squareColor;
                } else {
                    return false;
                }
            }
        }
        return knights == 0 || (knights == 1 && bishopSquareColor == -1);
    }

    public boolean requiresPromotion(ChessPosition sourcePosition, ChessPosition targetPosition) {
        if (sourcePosition == null || targetPosition == null) {
            return false;
        }
        Position source = sourcePosition.toPosition();
        Position target = targetPosition.toPosition();
        ChessPiece piece = (ChessPiece) board.piece(source);
        return promotes(piece, target) && piece.getColor() == currentPlayer
                && legalMoves(source, piece)[target.getRow()][target.getColumn()];
    }

    private boolean promotes(ChessPiece piece, Position target) {
        return piece instanceof Pawn && target.getRow() == (piece.getColor() == Color.WHITE ? 0 : 7);
    }

    private ChessPiece promotedPiece(Color color, Promotion promotion) {
        return switch (promotion) {
            case QUEEN -> new Queen(board, color);
            case ROOK -> new Rook(board, color);
            case BISHOP -> new Bishop(board, color);
            case KNIGHT -> new Knight(board, color);
        };
    }

    private boolean[][] candidateMoves(Position source, ChessPiece piece) {
        boolean[][] moves = piece.possibleMoves();
        if (piece instanceof Pawn && enPassantTarget != null) {
            int direction = piece.getColor() == Color.WHITE ? -1 : 1;
            if (enPassantTarget.getRow() == source.getRow() + direction
                    && Math.abs(enPassantTarget.getColumn() - source.getColumn()) == 1) {
                moves[enPassantTarget.getRow()][enPassantTarget.getColumn()] = true;
            }
        }
        int homeRow = piece.getColor() == Color.WHITE ? 7 : 0;
        if (piece instanceof King && piece.getMoveCount() == 0 && source.getRow() == homeRow
                && source.getColumn() == 4 && !isInCheck()) {
            addCastlingMove(moves, source, 1);
            addCastlingMove(moves, source, -1);
        }
        return moves;
    }

    private void addCastlingMove(boolean[][] moves, Position source, int direction) {
        int row = source.getRow();
        int rookColumn = direction > 0 ? 7 : 0;
        if (!hasUnmovedRook(row, rookColumn, currentPlayer)) {
            return;
        }
        int transitColumn = source.getColumn() + direction;
        for (int column = transitColumn; column != rookColumn; column += direction) {
            if (board.piece(row, column) != null) {
                return;
            }
        }
        Position transit = new Position(row, transitColumn);
        if (!wouldLeaveKingInCheck(new Move(source, transit))) {
            moves[row][transitColumn + direction] = true;
        }
    }

    private boolean hasUnmovedRook(int row, int column, Color color) {
        return board.piece(row, column) instanceof Rook rook
                && rook.getColor() == color && rook.getMoveCount() == 0;
    }

    private Move describeMove(Position source, Position target, ChessPiece piece) {
        int columnDistance = target.getColumn() - source.getColumn();
        if (piece instanceof King && Math.abs(columnDistance) == 2) {
            int row = source.getRow();
            Position rookSource = new Position(row, columnDistance > 0 ? 7 : 0);
            Position rookTarget = new Position(row, source.getColumn() + Integer.signum(columnDistance));
            return new Move(source, target, target, rookSource, rookTarget);
        }
        if (piece instanceof Pawn && columnDistance != 0 && board.piece(target) == null) {
            Position capturedPawn = new Position(source.getRow(), target.getColumn());
            return new Move(source, target, capturedPawn, null, null);
        }
        return new Move(source, target);
    }

    private boolean hasLegalMove() {
        for (int row = 0; row < board.getRows(); row++) {
            for (int column = 0; column < board.getColumns(); column++) {
                ChessPiece piece = (ChessPiece) board.piece(row, column);
                if (piece != null && piece.getColor() == currentPlayer
                        && hasDestination(legalMoves(new Position(row, column), piece))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasDestination(boolean[][] moves) {
        for (boolean[] row : moves) {
            for (boolean available : row) {
                if (available) {
                    return true;
                }
            }
        }
        return false;
    }

    private ChessPiece makeMove(Move move) {
        ChessPiece piece = (ChessPiece) board.removePiece(move.source());
        ChessPiece capturedPiece = (ChessPiece) board.removePiece(move.captureSquare());
        board.placePiece(piece, move.target());
        if (move.rookSource() != null) {
            board.placePiece(board.removePiece(move.rookSource()), move.rookTarget());
        }
        return capturedPiece;
    }

    private void undoMove(Move move, ChessPiece capturedPiece) {
        board.placePiece(board.removePiece(move.target()), move.source());
        if (move.rookSource() != null) {
            board.placePiece(board.removePiece(move.rookTarget()), move.rookSource());
        }
        if (capturedPiece != null) {
            board.placePiece(capturedPiece, move.captureSquare());
        }
    }

    private boolean wouldLeaveKingInCheck(Move move) {
        ChessPiece capturedPiece = makeMove(move);
        try {
            return isInCheck(currentPlayer);
        } finally {
            undoMove(move, capturedPiece);
        }
    }

    private boolean isInCheck(Color color) {
        Position kingPosition = findKing(color);
        for (int row = 0; row < board.getRows(); row++) {
            for (int column = 0; column < board.getColumns(); column++) {
                ChessPiece piece = (ChessPiece) board.piece(row, column);
                if (piece != null && piece.getColor() != color && piece.possibleMove(kingPosition)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Position findKing(Color color) {
        for (int row = 0; row < board.getRows(); row++) {
            for (int column = 0; column < board.getColumns(); column++) {
                if (board.piece(row, column) instanceof King king && king.getColor() == color) {
                    return new Position(row, column);
                }
            }
        }
        throw new IllegalStateException("Missing king: " + color);
    }

    private ChessPiece requireCurrentPlayerPieceAt(Position source) {
        ChessPiece piece = (ChessPiece) board.piece(source);
        if (piece == null) {
            throw new ChessException("A casa de origem está vazia.");
        }
        if (piece.getColor() != currentPlayer) {
            throw new ChessException("Escolha uma peça da cor que está jogando.");
        }
        return piece;
    }

    private void initialSetup() {
        placeBackRank(1, Color.WHITE);
        placeBackRank(8, Color.BLACK);
        for (char column = 'a'; column <= 'h'; column++) {
            placeNewPiece(column, 2, new Pawn(board, Color.WHITE));
            placeNewPiece(column, 7, new Pawn(board, Color.BLACK));
        }
    }

    private void placeBackRank(int row, Color color) {
        placeNewPiece('a', row, new Rook(board, color));
        placeNewPiece('b', row, new Knight(board, color));
        placeNewPiece('c', row, new Bishop(board, color));
        placeNewPiece('d', row, new Queen(board, color));
        placeNewPiece('e', row, new King(board, color));
        placeNewPiece('f', row, new Bishop(board, color));
        placeNewPiece('g', row, new Knight(board, color));
        placeNewPiece('h', row, new Rook(board, color));
    }

    private void placeNewPiece(char column, int row, ChessPiece piece) {
        board.placePiece(piece, new ChessPosition(column, row).toPosition());
    }
}
