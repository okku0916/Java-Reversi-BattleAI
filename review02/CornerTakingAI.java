package j2.review02;

import java.util.ArrayList;
import java.util.Random;

// 具体的なAI
public class CornerTakingAI extends AI {

    protected final Random random; // 乱数生成器

    // 色colorのプレイヤーのリバーシAIを生成する．
    // timeLimitedFlagがtrueの場合，時間制限が設定されている．
    public CornerTakingAI(int color, boolean timeLimitedFlag) {
        super(color, timeLimitedFlag);
        random = new Random();
    }

    // 局面boardに対する手を計算し，石を置くマスの座標を返す．
    // 隅が可能であれば，そこ(複数あればランダム)に石を置く．
    // そうでなければ，可能な手をランダムに選ぶ．
    @Override
    public Location compute(Board board) {
        var locs = new ArrayList<Location>();
        for (var y = 0; y < 8; y += 7) {
            for (var x = 0; x < 8; x += 7) {
                var l = new Location(x, y);
                if (board.isLegal(l)) {
                    locs.add(l);
                }
            }
        }
        if (locs.size() > 0) {
            return locs.get(random.nextInt(locs.size()));
        }
        locs = board.enumerateLegalLocations();
        return locs.get(random.nextInt(locs.size()));
    }

}