package j2.review02;

// Eインポートを追加
import j2.review02.s24kXXXX.*; // 自分のに変更

// AI同士の対戦シミュレーションを行い、勝率を計算するプログラム
public class AITournament {
    //
    // AI実行スレッド (Reversi.javaから移植し、コンソール用に修正)
    //
    public static class AIThread extends Thread {
        private final AI ai; // 実行するAIインスタンス
        private final Board board; // 局面
        private Location result; // 計算結果

        public AIThread(AI ai, Board board) {
            this.ai = ai;
            // Boardをディープコピーして、AIが元のBoardを勝手に変更するのを防ぐ
            this.board = new Board(board); 
        }

        public void run() {
            // ここでAI.getTime()が参照するスレッドの実行が始まる
            result = ai.compute(board); 
        }

        public Location getResult() {
            return result;
        }
    }

    //
    // クラスフィールド
    //

    // プレイヤーの種類の文字列の配列 (Reversi.javaからコピー)
    public static final String[] PLAYER_NAMES = {
        "人", "RandomAI", "CornerTakingAI", "PieceMaximizingAI", 
        "ChoiceMaximizingAI", "PieceMinimaxAI(4)", "PieceMinimaxAI(6)", 
        "MyAI"
    };

    //
    // クラスメソッド
    //

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
                // MyAIのインスタンス生成
                ai = new MyAI(color, timeLimitedFlag); 
                break;
            case 8:
                // プレイヤを追加できる
                // ai = new Player(color, timeLimitedFlag);
                break;
            default:
                System.err.printf("警告: AIタイプ %d はトーナメントではサポートされていません。\n", type);
        }
        return ai;
    }

    public static String getColorName(int color) {
        return (color == 0) ? "黒" : (color == 1 ? "白" : "" + color);
    }
    
    /**
     * AI同士の1対戦を実行する。（AIスレッド使用版）
     */
    public static int playOneGame(AI blackAI, AI whiteAI, boolean timeLimitedFlag) {
        Board board = new Board(); 
        if (blackAI == null || whiteAI == null) return -2;

        while (true) {
            int currentColor = board.getCurrentColor();
            AI currentAI = (currentColor == 0) ? blackAI : whiteAI;
            String currentColorName = getColorName(currentColor);
            
            // プレイ可能かチェック
            if (!board.isLegal()) {
                board.pass();
                if (!board.isLegal()) {
                    int bc = board.getCount(0);
                    int wc = board.getCount(1);
                    if (bc > wc) return 0; // 黒の勝ち
                    if (bc < wc) return 1; // 白の勝ち
                    return -1; // 引き分け
                }
                continue;
            }
            
         // AIを別スレッドで実行
            AIThread aiThread = new AIThread(currentAI, board);
            aiThread.start();
            
            Location move = null;
            // 最大待機時間をAIの持ち時間（5秒）より少し長く設定する（例: 5.5秒）
            // MyAIがTIME_LIMITをチェックしているため、この時間内に戻ることを期待する
            final long JOIN_TIME_MS = (long)(AI.TIME_LIMIT * 1.1 / 1.0e6); // 1.1倍 (5500ミリ秒)

            
            try {
                // AIスレッドの終了を待つ
                aiThread.join(JOIN_TIME_MS); 
            } catch (InterruptedException e) {
                // メインスレッドが中断された場合
                Thread.currentThread().interrupt(); // 割り込み状態を復元
            }

            // AIスレッドが生きている = 持ち時間内に戻らなかった
            if (aiThread.isAlive()) {
                // ここで Thread.stop() は呼ばず、時間切れとして処理を確定させる

                // 待機時間を超えて戻らない場合は、反則負けと見なす
                // 注意: スレッドは走り続けるが、結果は無視される
                System.out.printf("時間超過: %s (Join時間 %.2f秒超え) の反則負け\n", 
                    currentColorName, JOIN_TIME_MS / 1000.0);
                
                // MyAIなどの内部ロジックが5秒で停止するはずなので、
                // ここに来た場合はAIのバグか、システムが遅すぎるかのどちらか
                return (currentColor == 0) ? 1 : 0; // 相手の勝ち
            }

            // 結果の取得と処理
            move = aiThread.getResult();
            
            if (move == null || !board.isLegal(move)) {
                System.out.printf("不正な手: %s の反則負け (手: %s)\n", currentColorName, move);
                return (currentColor == 0) ? 1 : 0; // 相手の勝ち
            }
            
            board.put(move);
        }
    }

    //
    // mainメソッド
    //
//    1 "RandomAI", 
//    2 "CornerTakingAI", 
//    3 "PieceMaximizingAI", 
//    4 "ChoiceMaximizingAI", 
//    5 "PieceMinimaxAI(4)", 
//    6 "PieceMinimaxAI(6)", 
//    7 "MyAI", 
    public static void main(String[] args) {
        final int BLACK_AI_TYPE = 7;
        final int WHITE_AI_TYPE = 6;
        final int NUM_GAMES = 100;    
        final boolean TIME_LIMITED = true; 
        
        String blackAIName = PLAYER_NAMES[BLACK_AI_TYPE];
        String whiteAIName = PLAYER_NAMES[WHITE_AI_TYPE];
        
        System.out.println("--- リバーシ AI 対戦トーナメント (スレッド修正版) ---");
        System.out.printf("黒 (A): %s (タイプ: %d) vs 白 (B): %s (タイプ: %d)\n", 
            blackAIName, BLACK_AI_TYPE, whiteAIName, WHITE_AI_TYPE);
        System.out.printf("総対戦回数: %d回, 時間制限: %s\n", NUM_GAMES, TIME_LIMITED ? "あり" : "なし");
        System.out.println("------------------------------------");

        // AIインスタンスの生成
        AI blackAI = createAI(BLACK_AI_TYPE, 0, TIME_LIMITED);
        AI whiteAI = createAI(WHITE_AI_TYPE, 1, TIME_LIMITED);

        if (blackAI == null || whiteAI == null) {
            System.err.println("致命的なエラー: AIインスタンスの生成に失敗しました。処理を中断します。");
            return;
        }

        int blackWins = 0; 
        int whiteWins = 0; 
        int draws = 0;
        int errors = 0;
        
        long totalStartTime = System.currentTimeMillis();

        for (int i = 1; i <= NUM_GAMES; i++) {
            System.out.printf("対戦 %d/%d: ", i, NUM_GAMES);
            int result = playOneGame(blackAI, whiteAI, TIME_LIMITED);

            if (result == 0) {
                blackWins++;
                System.out.println("黒 (A) の勝ち");
            } else if (result == 1) {
                whiteWins++;
                System.out.println("白 (B) の勝ち");
            } else if (result == -1) {
                draws++;
                System.out.println("引き分け");
            } else {
                errors++;
                System.out.println("エラー/反則負け");
            }
        }
        
        long totalEndTime = System.currentTimeMillis();
        
        // 結果表示
        System.out.println("\n--- 対戦結果 ---");
        System.out.printf("合計実行時間: %.2f秒\n", (totalEndTime - totalStartTime) / 1000.0);
        System.out.printf("黒 (A: %s) の勝利: %d回\n", blackAIName, blackWins);
        System.out.printf("白 (B: %s) の勝利: %d回\n", whiteAIName, whiteWins);
        System.out.printf("引き分け: %d回\n", draws);
        System.out.printf("エラー/反則負け: %d回\n", errors);
        
        double totalDecidedGames = blackWins + whiteWins + draws;
        if (totalDecidedGames > 0) {
            double blackWinRate = (double) blackWins / totalDecidedGames * 100.0;
            double whiteWinRate = (double) whiteWins / totalDecidedGames * 100.0;
            
            System.out.println("----------------");
            System.out.printf("黒 (A) の勝率: %.2f%%\n", blackWinRate);
            System.out.printf("白 (B) の勝率: %.2f%%\n", whiteWinRate);
        }
        System.out.println("----------------");
    }
}
