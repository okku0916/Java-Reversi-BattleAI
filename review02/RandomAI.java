package j2.review02;

import java.util.ArrayList;
import java.util.Random;

// 具体的なAI
public class RandomAI extends AI {

    protected final Random random; // 乱数生成器

    // 色colorのプレイヤーのリバーシAIを生成する．
    // timeLimitedFlagがtrueの場合，時間制限が設定されている．
    public RandomAI(int color, boolean timeLimitedFlag) {
        super(color, timeLimitedFlag);
        random = new Random();
    }

    // 局面boardに対する手を計算し，石を置くマスの座標を返す．
    // 可能な手をランダムに選ぶ．
    @Override
    public Location compute(Board board) {
        var locs = board.enumerateLegalLocations();
        return locs.get(random.nextInt(locs.size()));
    }

}