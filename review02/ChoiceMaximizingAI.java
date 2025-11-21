package j2.review02;

import java.util.ArrayList;
import java.util.Random;

// 具体的なAI
public class ChoiceMaximizingAI extends AI {

    protected final Random random; // 乱数生成器

    // 色colorのプレイヤーのリバーシAIを生成する．
    // timeLimitedFlagがtrueの場合，時間制限が設定されている．
    public ChoiceMaximizingAI(int color, boolean timeLimitedFlag) {
        super(color, timeLimitedFlag);
        random = new Random();
    }

    // 局面boardに対する手を計算し，石を置くマスの座標を返す．
    // 1手後に相手がパスをすると仮定して(実際には通常はパスできない)，
    // 2手後に自分が石を置けるマスの個数が最大になるような手を選ぶ．
    @Override
    public Location compute(Board board) {
        var max = Integer.MIN_VALUE;
        var maxLocs = new ArrayList<Location>();
        var locs = board.enumerateLegalLocations();
        for (var i = 0; i < locs.size(); i++) {
            var loc = locs.get(i);
            board.put(loc);
            board.pass();
            var count = board.enumerateLegalLocations().size();
            board.undo();
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