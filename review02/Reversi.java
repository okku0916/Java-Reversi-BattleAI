package j2.review02;

import java.lang.management.ManagementFactory;

import javax.swing.JOptionPane;

import j2.review02.s24k0111.A5Ver1;
import j2.review02.s24k0111.A5Ver2;
import j2.review02.s24k0111.Egaroucid;
import j2.review02.s24k0111.MyAI;
import j2.review02.s24k0111.okkuVer2;
import j2.review02.s24k0111.okkuVer4;
import processing.core.PApplet;
import processing.core.PVector;

// リバーシ本体
public class Reversi extends PApplet {

    // AIスレッド
    public class AIThread extends Thread {

        private final int type; // AIの種類
        private final int color; // プレイヤーの色
        private final boolean timeLimitedFlag; // 時間制限するかどうか
        private final Board board; // 局面
        private Location result; // 計算結果

        // AIスレッドを生成する．
        // 引数としてAIの種類type，プレイヤーの色color，
        // 時間制限するかどうかtimeLimitedFlag，局面boardを受け取る．
        public AIThread(int type, int color, boolean timeLimitedFlag,
                Board board) {
            this.type = type;
            this.color = color;
            this.timeLimitedFlag = timeLimitedFlag;
            this.board = new Board(board);
        }

        // AIを実行する．
        public void run() {
            var ai = Reversi.createAI(type, color, timeLimitedFlag);
            result = ai.compute(board);
        }

    }

    //
    // クラスフィールド
    //

    // プレイヤーの種類の文字列の配列
    public static final String[] PLAYER_NAMES = {
        "人",                 // 0
        "RandomAI",           // 1
        "CornerTakingAI",     // 2
        "PieceMaximizingAI",  // 3
        "ChoiceMaximizingAI", // 4
        "PieceMinimaxAI(4)",  // 5
        "PieceMinimaxAI(6)",  // 6
        "MyAI",                // 7
        "Egaroucid",
        "A5Ver1",
        "okkuVer2",
        "A5Ver2",
        "okkuVer4"
    };

    public static final int SCREEN_SIZE = 500; // 画面サイズ
    public static final int BOARD_COLOR = 0xff00a000; // 盤面の色
    public static final int LINE_WEIGHT = 2; // 線の太さ
    public static final int PIECE_OUTLINE_WEIGHT = 2; // 石の枠の太さ
    public static final int TEXT_SIZE = 16; // 文字サイズ
    public static final float BOARD_OFFSET = 0.05f * SCREEN_SIZE; // 盤面の余白
    public static final float BOARD_SIZE = 0.9f * SCREEN_SIZE; // 盤面のサイズ
    public static final float PIECE_SIZE = BOARD_SIZE / 10; // 石のサイズ

    //
    // クラスメソッド
    //

    // AIを生成する．
    // 引数としてAIの種類type，プレイヤーの色color，
    // 時間制限するかどうかtimeLimitedFlagを受け取る．
    public static AI createAI(int type, int color, boolean timeLimitedFlag) {
        AI ai = null;
        switch (type) {
            case 1:
                ai = new RandomAI(color, timeLimitedFlag);
                break;
            case 2:
                ai = new CornerTakingAI(color, timeLimitedFlag);
                break;
            case 3:
                ai = new PieceMaximizingAI(color, timeLimitedFlag);
                break;
            case 4:
                ai = new ChoiceMaximizingAI(color, timeLimitedFlag);
                break;
            case 5:
                ai = new PieceMinimaxAI(color, timeLimitedFlag, 4);
                break;
            case 6:
                ai = new PieceMinimaxAI(color, timeLimitedFlag, 6);
                break;
            case 7:
                ai = new MyAI(color, timeLimitedFlag); // 引数を変えないこと
                break;
            case 8:
            	ai = new Egaroucid(color, timeLimitedFlag);
            	break;
            case 9:
            	ai = new A5Ver1(color, timeLimitedFlag);
            	break;
            case 10:
            	ai = new okkuVer2(color, timeLimitedFlag);
            	break;
            case 11:
            	ai = new A5Ver2(color, timeLimitedFlag);
            	break;
            case 12:
            	ai = new okkuVer4(color, timeLimitedFlag);
            	break;
        }
        return ai;
    }

    // 色colorの文字列を返す．
    public static String getColorName(int color) {
        return (color == 0) ? "黒" : (color == 1 ? "白" : "" + color);
    }

    // マス(x, y)の座標を返す．
    public static PVector transformBoardLocation(int x, int y) {
        return new PVector(BOARD_OFFSET + BOARD_SIZE * (x + 0.5f) / 8,
                BOARD_OFFSET + BOARD_SIZE * (y + 0.5f) / 8);
    }

    // 座標(x, y)のマスを返す．
    public static Location transformScreenPosition(float x, float y) {
        return new Location(
                (int) Math.floor(8 * (x - BOARD_OFFSET) / BOARD_SIZE),
                (int) Math.floor(8 * (y - BOARD_OFFSET) / BOARD_SIZE));
    }

    //
    // インスタンスフィールド
    //

    private int[] playerTypes; // 先手・後手のプレイヤー
    private boolean timeLimitedFlag; // 時間制限するかどうか
    private Board board; // 局面
    private AIThread aiThread; // AIスレッド
    private boolean mouseClickedFlag; // マウスがクリックされたかどうか
    private boolean gameOverFlag; // ゲームオーバーかどうか
    private boolean resultToShowFlag; // 結果を表示するかどうか

    //
    // インスタンスメソッド
    //

    // Processingのsettingsメソッド
    @Override
    public void settings() {
        size(SCREEN_SIZE, SCREEN_SIZE);
    }

    // Processingのsetupメソッド
    @Override
    public void setup() {
        var dialog = new StartDialog(PLAYER_NAMES);
        playerTypes = new int[2];
        for (var i = 0; i < 2; i++) {
            playerTypes[i] = dialog.getPlayerType(i);
            System.out.println((i == 0 ? "黒" : "白") + "："
                    + PLAYER_NAMES[playerTypes[i]]);
        }
        timeLimitedFlag = dialog.isTimeLimited();
        board = new Board();
        aiThread = null;
        mouseClickedFlag = false;
        gameOverFlag = false;
    }

    // AIスレッドを終了する．
    @SuppressWarnings("deprecation")
    private void killAIThread() {
        aiThread.stop();
        aiThread = null;
    }

    // マス(x, y)に色colorの石を描画する．
    // 追加の引数として不透明度alpha，枠の幅の重みoutlineを受け取る．
    private void drawPiece(int x, int y, int color, float alpha,
            float outline) {
        if (outline >= 0) {
            strokeWeight(PIECE_OUTLINE_WEIGHT * outline);
            stroke(255 * (1 - color));
        } else {
            noStroke();
        }
        var c = 255 * color;
        fill(c, c, c, 255 * alpha);
        var v = transformBoardLocation(x, y);
        ellipse(v.x, v.y, PIECE_SIZE, PIECE_SIZE);
    }

    // 局面boardの盤面を描画する．
    private void drawBoard(Board board) {
        background((board.getCurrentColor() == 0) ? 64 : 192);
        noStroke();
        fill(BOARD_COLOR);
        rect(BOARD_OFFSET, BOARD_OFFSET, BOARD_SIZE, BOARD_SIZE);
        strokeWeight(LINE_WEIGHT);
        stroke(0);
        for (var i = 0; i <= 8; i++) {
            var p = BOARD_OFFSET + BOARD_SIZE * i / 8;
            line(BOARD_OFFSET, p, BOARD_OFFSET + BOARD_SIZE, p);
            line(p, BOARD_OFFSET, p, BOARD_OFFSET + BOARD_SIZE);
        }
        fill((board.getCurrentColor() == 0) ? 255 : 0);
        textSize(TEXT_SIZE);
        textAlign(CENTER, CENTER);
        for (var i = 0; i < 8; i++) {
            var p = BOARD_OFFSET + BOARD_SIZE * (i + 0.5f) / 8;
            text(i, p, 0.025f * SCREEN_SIZE);
            text(i, 0.025f * SCREEN_SIZE, p);
        }
        text("Black " + board.getCount(0) + " - " +
                + board.getCount(1) + " White",
                0.5f * SCREEN_SIZE, 0.975f * SCREEN_SIZE);
        for (var y = 0; y < 8; y++) {
            for (var x = 0; x < 8; x++) {
                var c = board.get(x, y);
                if (c != -1) {
                    drawPiece(x, y, c, 1, -1);
                }
            }
        }
        var recs = board.getRecords();
        if (recs.size() > 0) {
            var lastRec = recs.get(recs.size() - 1);
            if (lastRec != null) {
                var color = lastRec.color();
                var placed = lastRec.placedPiece();
                drawPiece(placed.x(), placed.y(), color, 1, 1.5f);
                for (var i = 0; i < lastRec.flippedPieces().size(); i++) {
                    var flipped = lastRec.flippedPieces().get(i);
                    drawPiece(flipped.x(), flipped.y(), color, 1, 1);
                }
            }
        }
    }

    // Processingのdrawメソッド
    @Override
    public void draw() {
        if (resultToShowFlag) {
            // 最終の盤面を表示させるために結果表示を1フレーム遅らせる．
            var bc = board.getCount(0);
            var wc = board.getCount(1);
            JOptionPane.showMessageDialog(null,
                    "黒 " + bc + " - " + wc + " 白\n" +
                    (bc > wc ? "黒の勝ち" :
                     (bc < wc ? "白の勝ち" : "引き分け")));
            resultToShowFlag = false;
        } else if (!gameOverFlag) {
            var nextTurnFlag = false;
            var playerType = playerTypes[board.getCurrentColor()];
            if (playerType != 0) {
                if (aiThread == null) {
                    aiThread = new AIThread(playerType,
                            board.getCurrentColor(), timeLimitedFlag, board);
                    aiThread.start();
                    drawBoard(board);
                } else if (!aiThread.isAlive()) {
                    Location l = aiThread.result;
                    if (board.isLegal(l)) {
                        board.put(l);
                        nextTurnFlag = true;
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "不正な手：" + l + "\n" +
                                getColorName(board.getCurrentColor()) +
                                "の反則負け");
                        gameOverFlag = true;
                    }
                    aiThread = null;
                } else if (timeLimitedFlag) {
                    var time = ManagementFactory.getThreadMXBean().
                        getThreadUserTime(aiThread.getId());
                    if (time > AI.TIME_LIMIT) {
                        killAIThread();
                        JOptionPane.showMessageDialog(null,
                                "時間超過：" + (time / 1.0e9) + "\n" +
                                getColorName(board.getCurrentColor()) +
                                "の反則負け");
                        gameOverFlag = true;
                    }
                }
            } else {
                var l = transformScreenPosition(mouseX, mouseY);
                if (l.x() < 0 || l.x() >= 8 || l.y() < 0 || l.y() >= 8 ||
                        !board.isLegal(l)) {
                    drawBoard(board);
                } else if (mouseClickedFlag) {
                    board.put(l);
                    nextTurnFlag = true;
                } else {
                    drawBoard(board);
                    drawPiece(l.x(), l.y(), board.getCurrentColor(), 0.5f, -1);
                }
            }
            if (nextTurnFlag) {
                if (!board.isLegal()) {
                    board.pass();
                    if (!board.isLegal()) {
                        board.undo();
                        gameOverFlag = true;
                        resultToShowFlag = true;
                    }
                }
                drawBoard(board);
                if (!gameOverFlag) {
                    playerType = playerTypes[board.getCurrentColor()];
                    if (playerType != 0) {
                        aiThread = new AIThread(playerType,
                                board.getCurrentColor(), timeLimitedFlag,
                                board);
                        aiThread.start();
                    }
                }
            }
        }
        mouseClickedFlag = false;
    }

    // ProcessingのmouseClickedメソッド
    @Override
    public void mouseClicked() {
        mouseClickedFlag = true;
    }

    // ProcessingのkeyPressedメソッド
    @Override
    public void keyPressed() {
        if (key == 's') {
            saveFrame("Reversi.png");
        } else if (key == 'r') {
            if (aiThread != null) {
                killAIThread();
            }
            board = new Board();
            gameOverFlag = false;
        }
    }

    //
    // mainメソッド
    //

    // Processingプログラムの起動
    public static void main(String[] args) {
        PApplet.main(Reversi.class.getName());
    }

}