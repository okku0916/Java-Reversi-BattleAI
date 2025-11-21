package j2.review02;

import java.util.ArrayList;
import java.util.Random;

// 具体的なAI
public class PieceMaximizingAI extends AI {

    protected final Random random; // 乱数生成器

    // 色colorのプレイヤーのリバーシAIを生成する．
    // timeLimitedFlagがtrueの場合，時間制限が設定されている．
    public PieceMaximizingAI(int color, boolean timeLimitedFlag) {
        super(color, timeLimitedFlag);
        random = new Random();
    }

    // 局面boardに対する手を計算し，石を置くマスの座標を返す．
    // 自分の石の個数が最大になるような手を選ぶ．
    @Override
    public Location compute(Board board) {
        var max = Integer.MIN_VALUE;
        var maxLocs = new ArrayList<Location>();
        var locs = board.enumerateLegalLocations();
        for (var i = 0; i < locs.size(); i++) {
            var loc = locs.get(i);
            board.put(loc);
            var count = board.getCount(color);
            board.undo();
            if (count > max) {
                max = count;
                maxLocs.clear();
                maxLocs.add(loc);
            } else if (count == max) {
                maxLocs.add(loc);
            }
        }
        return maxLocs.get(random.nextInt(maxLocs.size()));
    }

}