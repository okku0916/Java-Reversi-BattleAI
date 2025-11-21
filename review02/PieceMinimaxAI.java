package j2.review02;

import java.util.ArrayList;
import java.util.Random;

// 具体的なAI
public class PieceMinimaxAI extends AI {

    protected final int depthLimit; // 探索の深さ制限
    protected final Random random; // 乱数生成器
    protected Location result; // 計算結果

    // 色colorのプレイヤーのリバーシAIを生成する．
    // timeLimitedFlagがtrueの場合，時間制限が設定されている．
    // 追加の引数として探索の深さ制限depthLimitを受け取る．
    public PieceMinimaxAI(int color, boolean timeLimitedFlag, int depthLimit) {
        super(color, timeLimitedFlag);
        this.depthLimit = depthLimit;
        random = new Random();
    }

    // 局面boardを評価する．
    protected int evaluate(Board board) {
        return board.getCount(color);
    }

    // 葉の局面boardを評価する．
    // 追加の引数として残りの深さremainingDepthを受け取る．
    protected int evaluateEnd(Board board, int remainingDepth) {
        var c = board.getCount(color);
        var d = c - board.getCount(Board.flip(color));
        return d > 0 ? c + 1000 * remainingDepth : c - 1000 * remainingDepth;
    }

    // マスのリストlocationsの要素をランダムに並べ替える．
    protected void randomizeLocations(ArrayList<Location> locations) {
        var copy = new ArrayList<Location>(locations);
        locations.clear();
        for (var i = copy.size(); i > 0; i--) {
            locations.add(copy.remove(random.nextInt(i)));
        }
    }

    // 評価値を最小化する．
    // 引数として局面board，残りの深さremainingDepthを受け取る．
    protected int minimize(Board board, int remainingDepth) {
        if (remainingDepth == 0) {
            return evaluate(board);
        }
        var locs = board.enumerateLegalLocations(); // 全ての置ける位置
        randomizeLocations(locs); // ランダムに並び替え
        if (locs.size() == 0) { // 置けるところがないならパスして次を調べる
            board.pass();
            var score = board.isLegal() ?
                maximize(board, remainingDepth - 1) :
                evaluateEnd(board, remainingDepth);
            board.undo();
            return score;
        }
        var min = Integer.MAX_VALUE;
        for (var i = 0; i < locs.size(); i++) {
            board.put(locs.get(i));
            var score = maximize(board, remainingDepth - 1);
            board.undo();
            if (score < min) {
                min = score;
            }
            if (timeLimitedFlag && remainingDepth >= 4 &&
                    getTime() > 0.95 * TIME_LIMIT) {
                break;
            }
        }
        return min;
    }

    // 評価値を最大化する．
    // 引数として局面board，残りの深さremainingDepthを受け取る．
    protected int maximize(Board board, int remainingDepth) {
        if (remainingDepth == 0) {
            return evaluate(board);
        }
        var locs = board.enumerateLegalLocations();
        randomizeLocations(locs);
        if (locs.size() == 0) {
            board.pass();
            var score = board.isLegal() ?
                minimize(board, remainingDepth - 1) :
                evaluateEnd(board, remainingDepth);
            board.undo();
            return score;
        }
        var max = Integer.MIN_VALUE;
        for (var i = 0; i < locs.size(); i++) {
            board.put(locs.get(i));
            var score = minimize(board, remainingDepth - 1);
            board.undo();
            if (score > max) {
                max = score;
                if (remainingDepth == depthLimit) {
                    result = locs.get(i);
                }
            }
            if (timeLimitedFlag && remainingDepth >= 4 &&
                    getTime() > 0.95 * TIME_LIMIT) {
                break;
            }
        }
        return max;
    }

    // 局面boardに対する手を計算し，石を置くマスの座標を返す．
    // 評価値に石数を用いたMinimax法によって手を選ぶ．
    @Override
    public Location compute(Board board) {
        result = null;
        maximize(board, depthLimit);
        return result;
    }

}