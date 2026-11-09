package application;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import application.ChessBoard.Square;
import chess.ChessException;
import chess.ChessMatch;
import chess.ChessPiece;
import chess.ChessPosition;
import chess.Color;
import chess.Promotion;

@SuppressWarnings("serial")
public final class ChessWindow extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final java.awt.Color BACKGROUND = new java.awt.Color(0x15221E);
    private static final java.awt.Color CARD = new java.awt.Color(0x22332D);
    private static final java.awt.Color BORDER = new java.awt.Color(0x3A5147);
    private static final java.awt.Color TEXT = new java.awt.Color(0xF7F1E1);
    private static final java.awt.Color MUTED = new java.awt.Color(0xBECEC0);
    private static final java.awt.Color GOLD = new java.awt.Color(0xF8DA85);
    private static final Font UI_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final String START_HINT = "Clique em uma peça da sua cor para ver os destinos disponíveis.";
    private final ChessBoard board = new ChessBoard(this::selectSquare);
    private final JLabel turnLabel = label(" ", MUTED, 13, Font.BOLD);
    private final JLabel statusLabel = label(" ", TEXT, 24, Font.BOLD);
    private final JTextArea message = textArea(UI_FONT, MUTED, 3);
    private final JTextArea capturedWhite = textArea(ChessBoard.symbolFont(22), TEXT, 2);
    private final JTextArea capturedBlack = textArea(ChessBoard.symbolFont(22), TEXT, 2);
    private final JButton claimButton = button("Pedir empate", false);
    private final JButton announceButton = button("Anunciar jogada para empate", false);
    private ChessMatch match = new ChessMatch();
    private Square selection;
    private boolean[][] destinations;
    private boolean announcing;
    private boolean finished;

    ChessWindow() {
        super(new BorderLayout(24, 18));
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 24, 24, 24));
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(label("PARTIDA LOCAL  /  DOIS JOGADORES", GOLD, 12, Font.BOLD), BorderLayout.NORTH);
        header.add(label("Xadrez", TEXT, 36, Font.BOLD), BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JPanel boardHolder = new JPanel(new GridBagLayout());
        boardHolder.setOpaque(false);
        boardHolder.add(board);
        add(boardHolder, BorderLayout.CENTER);
        add(createSidebar(), BorderLayout.EAST);

        claimButton.setName("claim-draw");
        announceButton.setName("announce-draw");
        claimButton.addActionListener(event -> claimCurrentPosition());
        announceButton.addActionListener(event -> toggleAnnouncement());
        statusLabel.setName("game-status");
        message.setName("game-message");
        capturedWhite.setName("captured-white");
        capturedBlack.setName("captured-black");
        refresh(START_HINT);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Xadrez · Partida local");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new ChessWindow());
            frame.pack();
            frame.setMinimumSize(frame.getSize());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setOpaque(false);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(282, 544));

        JPanel status = card();
        status.add(turnLabel);
        status.add(Box.createVerticalStrut(4));
        status.add(statusLabel);
        status.add(Box.createVerticalStrut(10));
        status.add(message);
        status.setMaximumSize(new Dimension(Integer.MAX_VALUE, 152));
        sidebar.add(status);
        sidebar.add(Box.createVerticalStrut(14));

        JPanel captures = card();
        captures.add(label("PEÇAS CAPTURADAS", GOLD, 12, Font.BOLD));
        captures.add(Box.createVerticalStrut(9));
        captures.add(label("Brancas", MUTED, 13, Font.PLAIN));
        captures.add(capturedWhite);
        captures.add(label("Pretas", MUTED, 13, Font.PLAIN));
        captures.add(capturedBlack);
        captures.setMaximumSize(new Dimension(Integer.MAX_VALUE, 212));
        sidebar.add(captures);
        sidebar.add(Box.createVerticalStrut(14));
        sidebar.add(Box.createVerticalGlue());

        sidebar.add(claimButton);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(announceButton);
        sidebar.add(Box.createVerticalStrut(8));
        JButton restart = button("Nova partida", true);
        restart.setName("new-game");
        restart.addActionListener(event -> startNewGame());
        sidebar.add(restart);
        return sidebar;
    }

    private void selectSquare(Square square) {
        if (finished) {
            return;
        }
        if (destinations != null && destinations[square.row()][square.column()]) {
            playSelectedMove(square.position());
            return;
        }
        ChessPiece piece = match.getPieces()[square.row()][square.column()];
        if (square.equals(selection)) {
            clearSelection();
            refresh(announcing ? "Selecione a peça da jogada que você quer anunciar." : START_HINT);
            return;
        }
        if (piece == null || piece.getColor() != match.getCurrentPlayer()) {
            refresh(selection == null ? "Selecione uma peça da cor que está jogando."
                    : "A casa " + square.position() + " não é um destino disponível. Escolha uma casa marcada.");
            return;
        }
        boolean[][] moves = match.possibleMoves(square.position());
        if (!hasDestination(moves)) {
            clearSelection();
            refresh("A peça em " + square.position() + " está sem jogadas legais. Selecione outra peça.");
            return;
        }
        selection = square;
        destinations = moves;
        refresh(announcing ? "Clique no destino para pedir empate com a jogada anunciada."
                : "Casa " + square.position() + " selecionada. Clique em um destino marcado.");
    }

    private static boolean hasDestination(boolean[][] moves) {
        for (boolean[] row : moves) {
            for (boolean available : row) {
                if (available) {
                    return true;
                }
            }
        }
        return false;
    }

    private void playSelectedMove(ChessPosition target) {
        ChessPosition source = selection.position();
        try {
            if (announcing) {
                match.claimDraw(source, target);
                announcing = false;
            } else {
                Promotion promotion = match.requiresPromotion(source, target) ? choosePromotion() : Promotion.QUEEN;
                if (promotion == null) {
                    return;
                }
                match.performChessMove(source, target, promotion);
            }
            clearSelection();
            refresh(START_HINT);
        } catch (ChessException exception) {
            announcing = false;
            clearSelection();
            refresh(exception.getMessage());
        }
    }

    private Promotion choosePromotion() {
        String[] choices = {"Dama", "Torre", "Bispo", "Cavalo"};
        int choice = JOptionPane.showOptionDialog(this, "Em qual peça o peão será promovido?",
                "Promoção", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);
        return choice < 0 ? null : Promotion.values()[choice];
    }

    private void claimCurrentPosition() {
        announcing = false;
        clearSelection();
        try {
            match.claimDraw();
            refresh(START_HINT);
        } catch (ChessException exception) {
            refresh(exception.getMessage());
        }
    }

    private void toggleAnnouncement() {
        announcing = !announcing;
        clearSelection();
        refresh(announcing ? "Selecione a peça e o destino da jogada que completaria o empate." : START_HINT);
    }

    private void startNewGame() {
        if (!finished && match.getTurn() > 1 && JOptionPane.showConfirmDialog(this,
                "Reiniciar a partida em andamento?", "Nova partida", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        match = new ChessMatch();
        announcing = false;
        clearSelection();
        refresh(START_HINT);
    }

    private void clearSelection() {
        selection = null;
        destinations = null;
    }

    private void refresh(String hint) {
        String side = match.getCurrentPlayer() == Color.WHITE ? "brancas" : "pretas";
        boolean mate = match.isCheckMate();
        boolean stalemate = !mate && match.isStalemate();
        ChessMatch.DrawReason draw = match.drawReason();
        finished = mate || stalemate || draw != null;
        turnLabel.setText(finished ? "PARTIDA ENCERRADA" : "TURNO " + match.getTurn());
        if (mate) {
            statusLabel.setText("Xeque-mate");
            message.setText(match.getCurrentPlayer() == Color.WHITE ? "As pretas venceram." : "As brancas venceram.");
        } else if (finished) {
            statusLabel.setText("Empate");
            message.setText(stalemate ? "O jogador da vez ficou sem jogadas legais e fora de xeque."
                    : "Empate por " + UI.drawDescription(draw) + ".");
        } else {
            statusLabel.setText(match.isInCheck() ? "Xeque nas " + side : "Vez das " + side);
            message.setText(hint);
        }
        message.setCaretPosition(0);
        List<ChessPiece> captured = match.getCapturedPieces();
        capturedWhite.setText(capturedSymbols(captured, Color.WHITE));
        capturedBlack.setText(capturedSymbols(captured, Color.BLACK));
        claimButton.setEnabled(!finished);
        announceButton.setEnabled(!finished);
        announceButton.setText(announcing ? "Cancelar anúncio de jogada" : "Anunciar jogada para empate");
        board.showPosition(match.getPieces(), selection, destinations, finished);
    }

    private static String capturedSymbols(List<ChessPiece> captured, Color color) {
        StringBuilder symbols = new StringBuilder();
        for (ChessPiece piece : captured) {
            if (piece.getColor() == color) {
                symbols.append(ChessBoard.symbol(piece, true)).append(' ');
            }
        }
        return symbols.isEmpty() ? "—" : symbols.toString();
    }

    private static JPanel card() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(14, 15, 14, 15)));
        card.setAlignmentX(LEFT_ALIGNMENT);
        return card;
    }

    private static JLabel label(String text, java.awt.Color color, int size, int style) {
        JLabel label = new JLabel(text);
        label.setFont(UI_FONT.deriveFont(style, (float) size));
        label.setForeground(color);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static JTextArea textArea(Font font, java.awt.Color color, int rows) {
        JTextArea area = new JTextArea(rows, 16);
        area.setFont(font);
        area.setForeground(color);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setAlignmentX(LEFT_ALIGNMENT);
        return area;
    }

    private static JButton button(String title, boolean primary) {
        JButton button = new JButton(title);
        button.setFont(UI_FONT.deriveFont(Font.BOLD));
        button.setBackground(primary ? GOLD : CARD);
        button.setForeground(primary ? BACKGROUND : TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(primary ? GOLD : BORDER),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
        button.setAlignmentX(LEFT_ALIGNMENT);
        return button;
    }
}
