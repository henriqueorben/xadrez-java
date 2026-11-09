package application;

import java.util.Scanner;

import chess.ChessException;
import chess.ChessMatch;
import chess.ChessPosition;
import chess.Promotion;

public class Program {
    public static void main(String[] args) {
        ChessMatch match = new ChessMatch();
        System.out.println("Para pedir empate, digite 'empate' ou 'empate ORIGEM DESTINO'.");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                UI.printBoard(match.getPieces());
                UI.printCapturedPieces(match.getCapturedPieces());
                if (match.isCheckMate()) {
                    UI.printCheckMate(match.getCurrentPlayer());
                    break;
                }
                if (match.isStalemate()) {
                    System.out.println("Empate por afogamento. O jogador da vez não tem jogadas legais.");
                    break;
                }
                ChessMatch.DrawReason draw = match.drawReason();
                if (draw != null) {
                    UI.printDraw(draw);
                    break;
                }
                UI.printTurn(match.getTurn(), match.getCurrentPlayer());
                if (match.isInCheck()) {
                    System.out.println("Xeque!");
                }
                try {
                    System.out.print("Origem: ");
                    if (!scanner.hasNextLine()) {
                        break;
                    }
                    String input = scanner.nextLine().strip();
                    String[] words = input.split("\\s+");
                    if (words[0].equalsIgnoreCase("empate")) {
                        claimDraw(match, words);
                        continue;
                    }
                    ChessPosition source = UI.readChessPosition(input);
                    UI.printBoard(match.getPieces(), match.possibleMoves(source));

                    System.out.print("Destino: ");
                    if (!scanner.hasNextLine()) {
                        break;
                    }
                    ChessPosition target = UI.readChessPosition(scanner.nextLine());

                    Promotion promotion = Promotion.QUEEN;
                    if (match.requiresPromotion(source, target)) {
                        System.out.print("Promoção [Q dama, R torre, B bispo, N cavalo; Enter = dama]: ");
                        if (scanner.hasNextLine()) {
                            promotion = UI.readPromotion(scanner.nextLine());
                        }
                    }
                    match.performChessMove(source, target, promotion);
                } catch (ChessException exception) {
                    System.out.println(exception.getMessage());
                }
            }
        }
    }

    private static void claimDraw(ChessMatch match, String[] words) {
        switch (words.length) {
            case 1 -> match.claimDraw();
            case 3 -> match.claimDraw(UI.readChessPosition(words[1]), UI.readChessPosition(words[2]));
            default -> throw new ChessException("Digite 'empate' ou 'empate ORIGEM DESTINO'.");
        }
    }
}
