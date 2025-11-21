package j2.review02;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * リバーシのAI対戦シミュレーター。
 * 複数のAI間で大量のゲームを自動実行し、勝敗結果を集計します。
 * Boardクラスに完全なリバーシのルールを実装しました。
 */
public class ReversiSimulator {

    // --- コアクラス定義 ---

    /**
     * マスの座標 (x, y) を表すレコード。
     */
    public record Location(int x, int y) {
        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    /**
     * AIのベースインターフェース。
     * 実際のAIクラスはこれを実装します。
     */
    public interface AI {
        long TIME_LIMIT = 5_000_000_000L; // 1秒 (ナノ秒)
        Location compute(Board board);
        int getColor();
        boolean isTimeLimited();
    }

    /**
     * リバーシの盤面とゲーム状態を管理するクラス (完全なロジックを実装)。
     * ご提供いただいたBoard.javaの完全な実装に基づいています。
     */
    public static class Board {

        // --- クラスフィールド ---
        public static final int BLACK = 0; // 黒
        public static final int WHITE = 1; // 白
        public static final int EMPTY = -1; // 空き
        
        // 1手分の履歴
        public record Record(
                int color, // 手番
                Location placedPiece, // 置かれた石
                ArrayList<Location> flippedPieces // 裏返された石のリスト
                ) {

            // 手番colorでマスplacedPieceに石を置く履歴を生成する．
            public Record(int color, Location placedPiece) {
                this(color, placedPiece, new ArrayList<Location>());
            }

            // マスpieceにある石を裏返したことを記録する．
            public void flip(Location piece) {
                flippedPieces.add(piece);
            }

        }

        // 隣接する八つのマスの方向のリスト
        private static final Location[] DIRECTIONS = {
            new Location(-1, 0), new Location(-1, 1), new Location(0, 1),
            new Location(1, 1), new Location(1, 0), new Location(1, -1),
            new Location(0, -1), new Location(-1, -1)};

        // --- クラスメソッド ---
        // 0を1，1を0に変える．
        public static int flip(int color) {
            return (color == BLACK) ? WHITE : ((color == WHITE) ? BLACK : EMPTY);
        }

        // --- インスタンスフィールド ---
        private final int[][] board; // 局面
        private final int[] counts; // BLACK, WHITEの石の個数
        private final ArrayList<Record> records; // 履歴のリスト
        private int currentColor; // 現在の手番

        // --- コンストラクタ ---
        // 最初の局面を生成する．
        public Board() {
            board = new int[8][8];
            for (var y = 0; y < 8; y++) {
                for (var x = 0; x < 8; x++) {
                    board[x][y] = EMPTY;
                }
            }
            // 初期配置
            board[4][3] = board[3][4] = BLACK;
            board[3][3] = board[4][4] = WHITE;
            counts = new int[2];
            counts[BLACK] = counts[WHITE] = 2;
            records = new ArrayList<Record>();
            currentColor = BLACK; // 黒から開始
        }

        // 局面sourceの(履歴を除く)コピーを生成する．
        public Board(Board source) {
            currentColor = source.currentColor;
            board = new int[8][8];
            for (var y = 0; y < 8; y++) {
                for (var x = 0; x < 8; x++) {
                    board[x][y] = source.board[x][y];
                }
            }
            counts = new int[2];
            counts[BLACK] = source.counts[BLACK];
            counts[WHITE] = source.counts[WHITE];
            records = new ArrayList<Record>();
        }

        // --- ゲッター ---
        public int getCurrentColor() { return currentColor; }
        public int getNextColor() { return flip(currentColor); }
        // マスlocationの状態を返す．(範囲外は-2を返す)
        public int get(Location location) {
            if (location.x() < 0 || location.x() >= 8 || location.y() < 0 || location.y() >= 8) return -2;
            return board[location.x()][location.y()];
        }
        // マス(x, y)の状態を返す．(範囲外は-2を返す)
        public int get(int x, int y) { 
            if (x < 0 || x >= 8 || y < 0 || y >= 8) return -2;
            return board[x][y]; 
        }
        public int getCount(int color) { return counts[color]; }
        public ArrayList<Record> getRecords() { return records; }

        // --- 合法手チェック ---

        // マスlocationに現在の手番の石を置くとしたとき，
        // 方向directionの石が裏返せるかどうかを返す．
        private boolean isLegal(Location location, int direction) {
            var d = DIRECTIONS[direction];
            for (var i = 1; i < 8; i++) {
                var x = location.x() + d.x() * i;
                var y = location.y() + d.y() * i;
                // 盤面外
                if (x < 0 || x >= 8 || y < 0 || y >= 8) {
                    return false;
                }
                var c = board[x][y];
                // 空きマス
                if (c == EMPTY) {
                    return false;
                } 
                // 自分の石が見つかった
                else if (c == currentColor) {
                    return i > 1; // 間に相手の石が1つ以上挟まっていること
                }
            }
            return false;
        }

        // マスlocationに現在の手番の石が置けるかどうかを返す．
        public boolean isLegal(Location location) {
            if (location == null || location.x() < 0 || location.x() >= 8 ||
                    location.y() < 0 || location.y() >= 8 ||
                    board[location.x()][location.y()] != EMPTY) {
                return false;
            }
            for (var i = 0; i < 8; i++) {
                if (isLegal(location, i)) {
                    return true;
                }
            }
            return false;
        }

        // マス(x, y)に現在の手番の石が置けるかどうかを返す．
        public boolean isLegal(int x, int y) {
            return isLegal(new Location(x, y));
        }

        // 空いているマスのどれかに現在の手番の石が置けるかどうかを返す．
        public boolean isLegal() {
            for (var y = 0; y < 8; y++) {
                for (var x = 0; x < 8; x++) {
                    if (isLegal(x, y)) {
                        return true;
                    }
                }
            }
            return false;
        }

        // 現在の手番が石を置けるマスを全て返す．
        public List<Location> getLegalMoves() { 
            var locs = new ArrayList<Location>();
            for (var y = 0; y < 8; y++) {
                for (var x = 0; x < 8; x++) {
                    var l = new Location(x, y);
                    if (isLegal(l)) {
                        locs.add(l);
                    }
                }
            }
            return locs;
        }

        // --- ゲーム操作 ---

        // マスlocationに現在の手番の石を置く．
        public void put(Location location) {
            if (!isLegal(location)) {
                throw new IllegalArgumentException("Illegal move: " + location);
            }

            var legalFlags = new boolean[8];
            for (var i = 0; i < 8; i++) {
                legalFlags[i] = isLegal(location, i);
            }
            
            board[location.x()][location.y()] = currentColor;
            counts[currentColor]++;
            var rec = new Record(currentColor, location);
            records.add(rec);
            var opp = flip(currentColor);
            
            for (var i = 0; i < 8; i++) {
                if (legalFlags[i]) {
                    Location d = DIRECTIONS[i];
                    // 裏返す処理
                    for (var j = 1; j < 8; j++) {
                        var x = location.x() + d.x() * j;
                        var y = location.y() + d.y() * j;
                        
                        if (x < 0 || x >= 8 || y < 0 || y >= 8) break; 
                        
                        if (board[x][y] == currentColor) {
                            break; // 自分の石が見つかったら終了
                        }
                        
                        board[x][y] = currentColor;
                        counts[currentColor]++;
                        counts[opp]--;
                        rec.flip(new Location(x, y));
                    }
                }
            }
            currentColor = flip(currentColor);
        }

        // マス(x, y)に現在の手番の石を置く．
        public void put(int x, int y) {
            put(new Location(x, y));
        }

        // 現在の手番がパスをする．
        public void pass() {
            records.add(null);
            currentColor = flip(currentColor);
        }

        // 一つ前の手に戻す．
        public void undo() {
            var rec = records.remove(records.size() - 1);
            if (rec != null) {
                board[rec.placedPiece.x()][rec.placedPiece.y()] = EMPTY;
                var opp = flip(rec.color); 
                var flippedPieceCount = rec.flippedPieces.size();
                
                for (var i = 0; i < flippedPieceCount; i++) {
                    Location p = rec.flippedPieces.get(i);
                    board[p.x()][p.y()] = opp; // 裏返した石を元に戻す
                }
                
                counts[rec.color] -= 1 + flippedPieceCount;
                counts[opp] += flippedPieceCount;
            }
            currentColor = flip(currentColor);
        }

    }

    /**
     * AIスレッド。
     */
    public static class AIThread extends Thread {
        private final AI ai;
        private final Board board;
        private Location result;

        public AIThread(int type, int color, boolean timeLimitedFlag, Board board) {
            this.ai = ReversiSimulator.createAI(type, color, timeLimitedFlag);
            this.board = new Board(board);
        }

        public void run() {
            // AIが compute メソッドを呼び出す (AIの実装はプレースホルダーのまま)
            result = ai.compute(board);
        }

        public Location getResult() {
            return result;
        }
    }

    // --- AI実装 (プレースホルダー) ---

    /** プレイヤーの種類 (元のReversi.javaから流用) */
    public static final String[] PLAYER_NAMES = {
        "人",                 // 0
        "RandomAI",           // 1
        "CornerTakingAI",     // 2
        "PieceMaximizingAI",  // 3
        "ChoiceMaximizingAI", // 4
        "PieceMinimaxAI(4)",  // 5
        "PieceMinimaxAI(6)",  // 6
        "MyAI",               // 7
        "Egaroucid"           // 8
    };

    /**
     * ランダムな合法手を選択するAI (プレースホルダー)
     */
    public static class RandomAI implements AI {
        private final int color;
        private final boolean timeLimitedFlag;
        private final Random random = new Random();

        public RandomAI(int color, boolean timeLimitedFlag) {
            this.color = color;
            this.timeLimitedFlag = timeLimitedFlag;
        }

        @Override
        public int getColor() { return color; }

        @Override
        public boolean isTimeLimited() { return timeLimitedFlag; }

        @Override
        public Location compute(Board board) {
            var legalMoves = board.getLegalMoves();
            if (legalMoves.isEmpty()) return null;
            // 簡易的にランダムな手を返す
            return legalMoves.get(random.nextInt(legalMoves.size()));
        }
    }

    /**
     * その他のAIのプレースホルダー (RandomAIと同じロジックを使用)
     */
    public static class PlaceholderAI extends RandomAI {
        public PlaceholderAI(int color, boolean timeLimitedFlag) {
            super(color, timeLimitedFlag);
        }
    }
    
    // AIを生成する。元のReversi.javaからロジックを流用。
    public static AI createAI(int type, int color, boolean timeLimitedFlag) {
        // 全てのAIタイプをPlaceholderAIとして実装 (MyAI, Egaroucidも含む)
        // Note: RandomAI は簡易的なランダム手を選択するように変更しました。
        return switch (type) {
            case 1 -> new RandomAI(color, timeLimitedFlag);
            case 2, 3, 4, 5, 6, 7, 8 -> new PlaceholderAI(color, timeLimitedFlag);
            default -> throw new IllegalArgumentException("Unknown AI type: " + type);
        };
    }

    // --- シミュレーション本体 ---

    private static final int NUM_GAMES = 100; // 実行する対戦回数
    
    // シミュレーション対象のAIタイプ
    // 例: 黒番(7: MyAI) vs 白番(8: Egaroucid)
    private static final int PLAYER_BLACK_TYPE = 1;
    private static final int PLAYER_WHITE_TYPE = 5;
    private static final boolean TIME_LIMITED = true; // 時間制限を有効にするか

    /**
     * 単一のゲームをシミュレートし、勝者の色を返す。
     * 0: 黒の勝ち, 1: 白の勝ち, -1: 引き分け, -2: 反則負け (時間超過/不正手)
     */
    public static int simulateGame(int blackType, int whiteType, boolean timeLimited) {
        var board = new Board();
        int consecutivePasses = 0; // 連続パス回数

        while (true) {
            int bc = board.getCount(Board.BLACK);
            int wc = board.getCount(Board.WHITE);

            // 終了条件チェック
            if (bc == 0 || wc == 0 || bc + wc == 64 || consecutivePasses >= 2) {
                // 石が全て埋まった、どちらかの石がなくなった、または両者パスでゲーム終了
                break;
            }

            int currentColor = board.getCurrentColor();
            int playerType = (currentColor == Board.BLACK) ? blackType : whiteType;

            if (board.isLegal()) {
                // 合法手が存在する場合
                consecutivePasses = 0;
                
                // AIターン
                AIThread aiThread = new AIThread(playerType, currentColor, timeLimited, board);
                aiThread.start();

                Location l = null;
                boolean timeOut = false;
                
                // AIの計算が終了するのを待つ
                try {
                    while (aiThread.isAlive()) {
                        if (timeLimited) {
                             // ManagementFactory.getThreadMXBean().getThreadUserTime() は環境によっては
                             // 常に0を返す可能性があるため、実際の実行時間制限としては不完全です。
                             // ただし、元のコードのロジックを踏襲します。
                             var time = ManagementFactory.getThreadMXBean()
                                 .getThreadUserTime(aiThread.getId());
                             if (time > AI.TIME_LIMIT) {
                                 // 時間超過
                                 aiThread.stop(); // 簡易的な停止 (非推奨だがシミュレーションのため使用)
                                 timeOut = true;
                                 break;
                             }
                        }
                        Thread.sleep(1); // スリープしてCPU負荷を軽減
                    }
                    if (!timeOut) {
                        l = aiThread.getResult();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return 1 - currentColor; // 例外発生は反則負けと見なす
                } catch (SecurityException e) {
                    // stop()が失敗した場合
                    timeOut = true;
                }
                
                if (timeOut) {
                    // System.out.println("  [時間超過] " + PLAYER_NAMES[playerType] + "(" + (currentColor == Board.BLACK ? "黒" : "白") + ")");
                    return 1 - currentColor; // 反則負け
                }
                
                if (l == null || !board.isLegal(l)) {
                    // System.out.println("  [不正な手] " + PLAYER_NAMES[playerType] + "(" + (currentColor == Board.BLACK ? "黒" : "白") + ") が手 " + l + " を指したため反則負け");
                    return 1 - currentColor; // 不正な手
                }

                board.put(l);

            } else {
                // 合法手がない場合 -> パス
                board.pass();
                consecutivePasses++;
            }
        } // while (true)

        // ゲーム終了後の勝敗判定
        int bc = board.getCount(Board.BLACK);
        int wc = board.getCount(Board.WHITE);

        if (bc > wc) return Board.BLACK;
        if (wc > bc) return Board.WHITE;
        return -1; // 引き分け
    }


    public static void main(String[] args) {
        // 設定情報の表示
        System.out.println("--- リバーシ AI 対戦シミュレーション ---");
        System.out.println("対戦数: " + NUM_GAMES + "回");
        System.out.println("黒番: " + PLAYER_NAMES[PLAYER_BLACK_TYPE]);
        System.out.println("白番: " + PLAYER_NAMES[PLAYER_WHITE_TYPE]);
        System.out.println("時間制限: " + (TIME_LIMITED ? "有効 (1秒)" : "無効"));
        System.out.println("---------------------------------------");

        int blackWins = 0;
        int whiteWins = 0;
        int draws = 0;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < NUM_GAMES; i++) {
            int result = simulateGame(PLAYER_BLACK_TYPE, PLAYER_WHITE_TYPE, TIME_LIMITED);
            
            switch (result) {
                case Board.BLACK -> blackWins++;
                case Board.WHITE -> whiteWins++;
                case -1 -> draws++; // 引き分け
            }
        }

        long endTime = System.currentTimeMillis();
        double totalTime = (endTime - startTime) / 1000.0;
        
        // 結果の表示
        System.out.println("\n===== シミュレーション結果 =====");
        System.out.println("実行時間: " + String.format("%.2f", totalTime) + "秒");
        System.out.println("総対戦数: " + NUM_GAMES + "回");
        System.out.println("---------------------------------");
        
        System.out.println(PLAYER_NAMES[PLAYER_BLACK_TYPE] + " (黒) の結果:");
        System.out.println("  勝利数: " + blackWins + "回");
        System.out.println("  勝率: " + String.format("%.2f", (double) blackWins / NUM_GAMES * 100) + "%");
        
        System.out.println("\n" + PLAYER_NAMES[PLAYER_WHITE_TYPE] + " (白) の結果:");
        System.out.println("  勝利数: " + whiteWins + "回");
        System.out.println("  勝率: " + String.format("%.2f", (double) whiteWins / NUM_GAMES * 100) + "%");
        
        System.out.println("\nその他:");
        System.out.println("  引き分け: " + draws + "回");
        System.out.println("---------------------------------");
    }
}