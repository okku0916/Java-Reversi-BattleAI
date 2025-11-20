// BEGIN
package j2.review02.s24kXXXX;

import java.util.ArrayList;
import java.util.Random;

import j2.review02.AI;
import j2.review02.Board;
import j2.review02.Location;

// 課題で作成するリバーシAI
public class okkuVer2 extends AI {
	protected final int depthLimit = 8; // 探索の深さ制限
    protected final Random random; // 乱数生成器
    protected Location result; // 計算結果
    protected int searchCount;
    private static final int[][] WEIGHTS = { // 盤面の重み
            { 100, -20, 10, 5, 5, 10, -20, 100 },
            { -20, -50, -2, -2, -2, -2, -50, -20 },
            { 10, -2, -1, -1, -1, -1, -2, 10 },
            { 5, -2, -1, -1, -1, -1, -2, 5 },
            { 5, -2, -1, -1, -1, -1, -2, 5 },
            { 10, -2, -1, -1, -1, -1, -2, 10 },
            { -20, -50, -2, -2, -2, -2, -50, -20 },
            { 100, -20, 10, 5, 5, 10, -20, 100 }
        };

    // 色colorのプレイヤーのリバーシAIを生成する．
    // timeLimitedFlagがtrueの場合，時間制限が設定されている．
    public okkuVer2(int color, boolean timeLimitedFlag) {
        super(color, timeLimitedFlag);
        random = new Random();
    }

    protected int evaluate(Board board) {
    	int score = 0; // 総合スコア
    	
    	// 盤面評価
    	int posScore = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int piece = board.get(x, y);
                if (piece == color) {
                    posScore += WEIGHTS[x][y];
                } else if (piece == Board.flip(color)) {
                    posScore -= WEIGHTS[x][y];
                }
            }
        }
        score += posScore;
        
        // 選択可能な手の数
    	ArrayList<Location> myMoves = board.enumerateLegalLocations();
        board.pass(); // パスして調べて戻す
        ArrayList<Location> oppMoves = board.enumerateLegalLocations();
        board.undo();
        int mobilityScore = myMoves.size() - oppMoves.size();
        
        score += mobilityScore;
        return score;
    }
    
    protected int evaluateEnd(Board board, int remainingDepth) {
        var c = board.getCount(color);
        var d = c - board.getCount(Board.flip(color));
        return d > 0 ? c + 1000 * remainingDepth : c - 1000 * remainingDepth;
    }
    
    protected void randomizeLocations(ArrayList<Location> locations) {
        var copy = new ArrayList<Location>(locations);
        locations.clear();
        for (var i = copy.size(); i > 0; i--) {
            locations.add(copy.remove(random.nextInt(i)));
        }
    }
    
    protected int minimize(Board board, int remainingDepth, int alpha, int beta) {
    	searchCount += 1;
        if (remainingDepth == 0) {
            return evaluate(board);
        }
        var locs = board.enumerateLegalLocations(); // 全ての置ける位置
        randomizeLocations(locs); // ランダムに並び替え
        // 置けるところがないならパスして次を調べるか、次もパスなら葉の評価を行う
        if (locs.size() == 0) { 
            board.pass();
            var score = board.isLegal() ?
                maximize(board, remainingDepth - 1, alpha, beta) :
                evaluateEnd(board, remainingDepth);
            board.undo();
            return score;
        }
        var min = Integer.MAX_VALUE;
        for (var i = 0; i < locs.size(); i++) {
            board.put(locs.get(i));
            var score = maximize(board, remainingDepth - 1, alpha, beta);
            board.undo();
            if (score < min) {
                min = score;
            }
            if (min <= alpha) { 
                return min; // このノードのmin値が親ノードのalpha(Maxの最善値)以下になったのでこれ以上探索しても無駄
            }
            if (min < beta) {
                beta = min;
            }
            if (timeLimitedFlag && remainingDepth >= 4 &&
                    getTime() > 0.95 * TIME_LIMIT) {
//            	System.out.println("実行時間制限");
                break;
            }
        }
        return min;
    }
    
    protected int maximize(Board board, int remainingDepth, int alpha, int beta) {
    	searchCount += 1;
        if (remainingDepth == 0) {
            return evaluate(board);
        }
        var locs = board.enumerateLegalLocations();
        randomizeLocations(locs);
        if (locs.size() == 0) {
            board.pass();
            var score = board.isLegal() ?
                minimize(board, remainingDepth - 1, alpha, beta) :
                evaluateEnd(board, remainingDepth);
            board.undo();
            return score;
        }
        var max = Integer.MIN_VALUE;
        for (var i = 0; i < locs.size(); i++) {
            board.put(locs.get(i));
            var score = minimize(board, remainingDepth - 1, alpha, beta);
            board.undo();
            if (score > max) {
                max = score;
                if (remainingDepth == depthLimit) {
                    result = locs.get(i);
                }
            }
            if (max >= beta) {
                return max; // このノードのmax値が親ノードのbeta(Minの最善値)以上になったのでこれ以上探索しても無駄
            }
            if (max > alpha) {
            	alpha = max;
            }
            
            if (timeLimitedFlag && remainingDepth >= 4 &&
                    getTime() > 0.95 * TIME_LIMIT) {
//            	System.out.println("実行時間制限");
                break;
            }
        }
        return max;
    }
    
    // 局面boardに対する手を計算し，石を置くマスの座標を返す．
    @Override
    public Location compute(Board board) {
    	result = null;
    	searchCount = 0;
        maximize(board, depthLimit, Integer.MIN_VALUE, Integer.MAX_VALUE);
//        System.out.println(searchCount);
        return result;
    }

}
// END
