package j2.review02;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

// プレイヤー選択のダイアログ
public class StartDialog extends JDialog
        implements ActionListener, ItemListener  {

    private final int[] playerTypes; // 先手・後手のプレイヤー
    private final JCheckBox timeLimitedCheckBox; // 時間制限のチェックボックス
    private boolean timeLimitedFlag; // 時間制限するかどうか

    // プレイヤーcolorについて種類を選択するパネルを生成する．
    // 追加の引数としてプレイヤーの種類の文字列の配列players，
    // デフォルトの種類defaultPlayerTypeを受け取る．
    private JPanel createSelecter(int color, String[] players,
            int defaultPlayerType) {
        var p = new JPanel();
        var t = (color == 0) ? "先手(黒)" : "後手(白)";
        p.setBorder(BorderFactory.createTitledBorder(t));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        var g = new ButtonGroup();
        for (var i = 0; i < players.length; i++) {
            var b = new JRadioButton(players[i]);
            b.setActionCommand("player:" + color + ":" + i);
            b.addActionListener(this);
            g.add(b);
            p.add(b);
            if (i == defaultPlayerType) {
                playerTypes[color] = i;
                b.setSelected(true);
            }
        }
        return p;
    }

    // プレイヤーを生成する．
    // 引数としてプレイヤーの種類の文字列の配列playersを受け取る．
    public StartDialog(String[] players) {
        super((JFrame) null, "Reversi", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        playerTypes = new int[2];
        var pane = getContentPane();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        var bw = new JPanel();
        bw.setLayout(new BoxLayout(bw, BoxLayout.X_AXIS));
        var black = createSelecter(0, players, 0);
        bw.add(black);
        var white = createSelecter(1, players, players.length - 1);
        bw.add(white);
        pane.add(bw);
        timeLimitedCheckBox = new JCheckBox("AIの時間制限(5秒)", true);
        timeLimitedCheckBox.addItemListener(this);
        timeLimitedCheckBox.setAlignmentX(CENTER_ALIGNMENT);
        timeLimitedFlag = true;
        pane.add(timeLimitedCheckBox);
        var start = new JButton("スタート");
        start.setActionCommand("start");
        start.addActionListener(this);
        start.setAlignmentX(CENTER_ALIGNMENT);
        pane.add(start);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // プレイヤーcolorの種類を返す．
    public int getPlayerType(int color) {
        return playerTypes[color];
    }

    // 時間制限するかどうかを返す．
    public boolean isTimeLimited() {
        return timeLimitedFlag;
    }

    // ボタンのイベントeventを処理する．
    @Override
    public void actionPerformed(ActionEvent event) {
        var command = event.getActionCommand().split(":");
        if (command[0].equals("start")) {
            dispose();
        } else if (command[0].equals("player")) {
            var c = Integer.parseInt(command[1]);
            var p = Integer.parseInt(command[2]);
            playerTypes[c] = p;
        }
    }

    // チェックボックスのイベントeventを処理する．
    @Override
    public void itemStateChanged(ItemEvent event) {
        var source = event.getItemSelectable();
        if (source == timeLimitedCheckBox) {
            timeLimitedFlag = timeLimitedCheckBox.isSelected();
        }
    }

}