package j2.review02;

import java.util.ArrayList;

// 局面と履歴
public class Board {

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

    //
    // クラスフィールド
    //

    // 隣接する八つのマスの方向のリスト
    private static final Location[] DIRECTIONS = {
        new Location(-1, 0), new Location(-1, 1), new Location(0, 1),
        new Location(1, 1), new Location(1, 0), new Location(1, -1),
        new Location(0, -1), new Location(-1, -1)};

    //
    // クラスメソッド
    //

    // 0を1，1を0に変える．
    public static int flip(int color) {
        return (color == 0) ? 1 : ((color == 1) ? 0 : -1);
    }

    //
    // インスタンスフィールド
    //

    private final int[][] board; // 局面
    private final int[] counts; // 0, 1の石の個数
    private final ArrayList<Record> records; // 履歴のリスト
    private int currentColor; // 現在の手番

    //
    // インスタンスメソッド
    //

    // 最初の局面を生成する．
    public Board() {
        board = new int[8][8];
        for (var y = 0; y < 8; y++) {
            for (var x = 0; x < 8; x++) {
                board[x][y] = -1;
            }
        }
        board[4][3] = board[3][4] = 0;
        board[3][3] = board[4][4] = 1;
        counts = new int[2];
        counts[0] = counts[1] = 2;
        records = new ArrayList<Record>();
        currentColor = 0;
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
        counts[0] = source.counts[0];
        counts[1] = source.counts[1];
        records = new ArrayList<Record>();
    }

    // 現在の手番を返す．
    public int getCurrentColor() {
        return currentColor;
    }

    // 次の手番を返す．
    public int getNextColor() {
        return flip(currentColor);
    }

    // マスlocationの状態を返す．
    public int get(Location location) {
        return board[location.x()][location.y()];
    }

    // マス(x, y)の状態を返す．
    public int get(int x, int y) {
        return board[x][y];
    }

    // 盤面上にあるcolorの石の個数を返す．
    public int getCount(int color) {
        return counts[color];
    }

    // 履歴のリストを返す．
    public ArrayList<Record> getRecords() {
        return records;
    }

    // マスlocationに現在の手番の石を置くとしたとき，
    // 方向directionの石が裏返せるかどうかを返す．
    private boolean isLegal(Location location, int direction) {
        var d = DIRECTIONS[direction];
        for (var i = 1; i < 8; i++) {
            var x = location.x() + d.x() * i;
            var y = location.y() + d.y() * i;
            if (x < 0 || x >= 8 || y < 0 || y >= 8) {
                return false;
            }
            var c = board[x][y];
            if (c == -1) {
                return false;
            } else if (c == currentColor) {
                return i > 1;
            }
        }
        return false;
    }

    // マスlocationに現在の手番の石が置けるかどうかを返す．
    public boolean isLegal(Location location) {
        if (location == null || location.x() < 0 || location.x() >= 8 ||
                location.y() < 0 || location.y() >= 8 ||
                board[location.x()][location.y()] != -1) {
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
    public ArrayList<Location> enumerateLegalLocations() {
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

    // マスlocationに現在の手番の石を置く．
    public void put(Location location) {
        var legalFlags = new boolean[8];
        var legal = false;
        for (var i = 0; i < 8; i++) {
            legalFlags[i] = isLegal(location, i);
            legal = legal || legalFlags[i];
        }
        board[location.x()][location.y()] = currentColor;
        counts[currentColor]++;
        var rec = new Record(currentColor, location);
        records.add(rec);
        var opp = flip(currentColor);
        for (var i = 0; i < 8; i++) {
            if (legalFlags[i]) {
                Location d = DIRECTIONS[i];
                for (var j = 1; j < 8; j++) {
                    var x = location.x() + d.x() * j;
                    var y = location.y() + d.y() * j;
                    if (board[x][y] == currentColor) {
                        break;
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
            board[rec.placedPiece.x()][rec.placedPiece.y()] = -1;
            var opp = (rec.color + 1) % 2;
            var flippedPieceCount = rec.flippedPieces.size();
            for (var i = 0; i < flippedPieceCount; i++) {
                Location p = rec.flippedPieces.get(i);
                board[p.x()][p.y()] = opp;
            }
            counts[rec.color] -= 1 + flippedPieceCount;
            counts[opp] += flippedPieceCount;
        }
        currentColor = flip(currentColor);
    }

}