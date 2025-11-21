// BEGIN
package j2.review02.s24k0111;

import java.util.ArrayList;
import java.util.Random;

import j2.review02.AI;
import j2.review02.Board;
import j2.review02.Location;

// 課題で作成するリバーシAI
public class MyAI extends AI {
	protected int depthLimit; // 探索の深さ制限
    protected final Random random; // 乱数生成器
    protected Location result; // 計算結果
    protected Location prevBestMove; // 反復深化で使う前回の最善手
    protected boolean timeUp; // 制限時間に達したかどうか
    protected int searchCount;
    private static final int[][] WEIGHTS = { // 盤面の重み
            { 30, -20, 0, -1, -1, 0, -20, 30 },
            { -20, -30, -3, -3, -3, -3, -30, -20 },
            { 0, -3, 0, -1, -1, 0, -3, 0 },
            { -1, -3, -1, -1, -1, -1, -3, -1 },
            { -1, -3, -1, -1, -1, -1, -3, -1 },
            { 0, -3, 0, -1, -1, 0, -3, 0 },
            { -20, -30, -3, -3, -3, -3, -30, -20 },
            { 30, -20, 0, -1, -1, 0, -20, 30 }
        };

    // 色colorのプレイヤーのリバーシAIを生成する．
    // timeLimitedFlagがtrueの場合，時間制限が設定されている．
    public MyAI(int color, boolean timeLimitedFlag) {
        super(color, timeLimitedFlag);
        random = new Random();
    }
    
    protected int getFrontierScore(Board board, int myColor) {
        int oppColor = Board.flip(myColor);
        int myFrontier = 0;
        int oppFrontier = 0;

        // 8方向の差分
        int[] dr = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dc = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int piece = board.get(r, c);
                if (piece != -1) {
                    boolean isFrontier = false;
                    // 8方向をチェック
                    for (int i = 0; i < 8; i++) {
                        int nr = r + dr[i];
                        int nc = c + dc[i];
                        // 盤外チェック
                        if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                            // 隣接するマスが空きマスかチェック
                            if (board.get(nr, nc) == -1) {
                                isFrontier = true;
                                break; // 1つでも空きマスに接していれば開放石
                            }
                        }
                    }
                    if (isFrontier) {
                        if (piece == myColor) {
                            myFrontier++;
                        } else {
                            oppFrontier++;
                        }
                    }
                }
            }
        }
        return oppFrontier - myFrontier;
    }
    
    protected int evaluate(Board board) {
    	int score = 0; // 総合スコア
    	int sumStone = board.getCount(color) + board.getCount(Board.flip(color));
    	int posWeight, mobilityWeight, diffWeight;
    	int frontierWeight;
    	
    	if (sumStone <= 20) { // 序盤は選択可能な手を重視
    	    posWeight = 1;
    	    mobilityWeight = 15; //12
    	    diffWeight = 0;
    	    frontierWeight = 12; //8
    	} else if (sumStone <= 48) {
    	    posWeight = 2; // 2
    	    mobilityWeight = 12; //10
    	    diffWeight = 2; //2
    	    frontierWeight = 22; // 20
    	} else if (sumStone <= 56) {
    	    posWeight = 1; 
    	    mobilityWeight = 3; //3
    	    diffWeight = 20; //20
    	    frontierWeight = 0;
    	} else {
    	    posWeight = 0;
    	    mobilityWeight = 0;
    	    diffWeight = 50; // 最終石差の重みを最大化
    	    frontierWeight = 0;
    	}
    	
    	// 囲い具合
    	int frontierScore = getFrontierScore(board, color);
    	score += frontierScore * frontierWeight;
    	
    	// 盤面評価
    	int posScore = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int stone = board.get(x, y);
                if (stone == color) {
                    posScore += WEIGHTS[x][y];
                } else if (stone == Board.flip(color)) {
                    posScore -= WEIGHTS[x][y];
                }
            }
        }
        
        score += posScore * posWeight;
        
        // 選択可能な手の数
    	ArrayList<Location> myMoves = board.enumerateLegalLocations();
        board.pass(); // パスして調べて戻す
        ArrayList<Location> oppMoves = board.enumerateLegalLocations();
        board.undo();
        int mobilityScore = myMoves.size() - oppMoves.size();
        score += mobilityScore * mobilityWeight;
        
        // 石の差
        int stoneDiff = board.getCount(color) - board.getCount(Board.flip(color));
        score += stoneDiff * diffWeight;
        
        return score;
    }
    
    protected int evaluateEnd(Board board, int remainingDepth) {
        var c = board.getCount(color);
        var d = c - board.getCount(Board.flip(color));
        return d > 0 ? c + 10000 * remainingDepth : c - 10000 * remainingDepth;
    }
    
    // 良い手から探索するようにする
    protected ArrayList<Location> bestLocations(Board board) {
    	var locs = board.enumerateLegalLocations();
    	if (locs.size() == 0) { // 有効手がないなら返す
    		return locs;
    	}
    	ArrayList<Integer> score = new ArrayList<>();
        for (var i = 0; i < locs.size(); i++) {
            board.put(locs.get(i));
            // ここが良い手の評価
//            score.add(board.getCount(color) - board.getCount(Board.flip(color))); // 石の差
//            score.add(WEIGHTS[locs.get(i).x()][locs.get(i).y()]); // 重み
        	score.add(evaluate(board));
            board.undo();
            
        }
        for (int i = 0; i < locs.size() - 1; i++) { // スコア順にソート
            for (int j = i + 1; j < locs.size(); j++) {
                if (score.get(i) < score.get(j)) {
                    int tmpScore = score.get(i);
                    score.set(i, score.get(j));
                    score.set(j, tmpScore);

                    Location tmpLoc = locs.get(i);
                    locs.set(i, locs.get(j));
                    locs.set(j, tmpLoc);
                }
            }
        }
        if (prevBestMove != null) { // 前回の最善手を最優先
            locs.remove(prevBestMove);
            locs.add(0, prevBestMove);
        }
        return locs;
    }
    
    protected int minimize(Board board, int remainingDepth, int alpha, int beta) {
    	searchCount += 1;
        if (remainingDepth == 0) {
            return evaluate(board);
        }
//        var locs = board.enumerateLegalLocations(); // 全ての置ける位置
//        randomizeLocations(locs); // ランダムに並び替え
        
        var locs = bestLocations(board); // 良い手順に並べた置ける位置
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
            if (score < min) { // 探索した評価値がminより低いなら更新
                min = score;
            }
            if (min <= alpha) { 
                return min; // このノードのmin値が親ノードのalpha(Maxの最善値)以下になったのでこれ以上探索しても無駄
            }
            if (min < beta) { // このノード以下で見つかる最悪手の評価
                beta = min;
            }
            if (timeLimitedFlag && remainingDepth >= 4 &&
                    getTime() > 0.95 * TIME_LIMIT) {
//            	System.out.println("実行時間制限");
            	timeUp = true;
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
//        var locs = board.enumerateLegalLocations();
//        randomizeLocations(locs);
        
        var locs = bestLocations(board);
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
        	Location m = locs.get(i);
            board.put(m);
            var score = minimize(board, remainingDepth - 1, alpha, beta);
            board.undo();
            if (score > max) { // 探索した評価値がmaxより高いなら更新
                max = score;
                if (remainingDepth == depthLimit) {
                    result = locs.get(i);
                }
            }
            if (max >= beta) {
                return max; // このノードのmax値が親ノードのbeta(Minの最善値)以上になったのでこれ以上探索しても無駄
            }
            if (max > alpha) { // このノード以下で見つかる最善手の評価
            	alpha = max;
            }
            
            if (timeLimitedFlag && remainingDepth >= 4 &&
                    getTime() > 0.95 * TIME_LIMIT) {
//            	System.out.println("実行時間制限");
            	timeUp = true;
                break;
            }
        }
        return max;
    }
    
    // 局面boardに対する手を計算し，石を置くマスの座標を返す．
    @Override
    public Location compute(Board board) {
    	timeUp = false;
    	prevBestMove = null; // 一つ前の深さの最善手
        Location finalResult = null; // 最終的に返す手

        var locs = bestLocations(board); 
        if (locs.size() == 1) {
        	return locs.get(0); // 一つしか有効手がないならすぐ返す
        }
        finalResult = locs.get(0); // 一応安全策
        
        // 反復深化
        for (int depth = 1; depth < 16; depth++) {
        	if (depth > 64 - (board.getCount(color) + board.getCount(Board.flip(color)))) { // 無駄な探索はしない
        		break;
        	}
            depthLimit = depth; // 深さを更新
            result = null; // この深さの結果をリセット
            maximize(board, depth, Integer.MIN_VALUE, Integer.MAX_VALUE);
            if (timeUp) { // 制限時間に達した深さで返された手は弱いかもしれないので更新しない
            	break;
            }
            if (result != null) { // 結果を更新
            	prevBestMove = result;
                finalResult = result;
            }
//            System.out.println("深さ" + depth + "の探索が終了しました");
        }
        return finalResult;
    }
}
// END