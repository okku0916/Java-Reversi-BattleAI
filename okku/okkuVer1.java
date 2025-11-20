// BEGIN
package j2.review02;

import java.util.ArrayList;
import java.util.Random;

import j2.review02.AI;
import j2.review02.Board;
import j2.review02.Location;

// 課題で作成するリバーシAI
public class okkuVer1 extends AI {
	protected final int depthLimit = 8; // 探索の深さ制限
    protected final Random random; // 乱数生成器
    protected Location result; // 計算結果

    // 色colorのプレイヤーのリバーシAIを生成する．
    // timeLimitedFlagがtrueの場合，時間制限が設定されている．
    public okkuVer2(int color, boolean timeLimitedFlag) {
        super(color, timeLimitedFlag);
        random = new Random();
    }

    protected int evaluate(Board board) {
    	int score = 0;
    	ArrayList<Location> myMoves = board.enumerateLegalLocations();
        board.pass();
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
        if (remainingDepth == 0) {
            return evaluate(board);
        }
        var locs = board.enumerateLegalLocations();
        randomizeLocations(locs);
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
                return min;
            }
            if (min < beta) {
                beta = min;
            }
            if (timeLimitedFlag && remainingDepth >= 4 &&
                    getTime() > 0.95 * TIME_LIMIT) {
                break;
            }
        }
        return min;
    }
    
    protected int maximize(Board board, int remainingDepth, int alpha, int beta) {
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
                return max;
            }
            if (max > alpha) {
            	alpha = max;
            }
            
            if (timeLimitedFlag && remainingDepth >= 4 &&
                    getTime() > 0.95 * TIME_LIMIT) {
                break;
            }
        }
        return max;
    }
    
    // 局面boardに対する手を計算し，石を置くマスの座標を返す．
    @Override
    public Location compute(Board board) {
    	result = null;
        maximize(board, depthLimit, Integer.MIN_VALUE, Integer.MAX_VALUE);
        return result;
    }

}
// END
