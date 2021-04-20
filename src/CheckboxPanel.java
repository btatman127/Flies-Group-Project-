import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionListener;

public class CheckboxPanel extends JPanel {
	private final JCheckBox[] boxes;
	private final boolean selected;
	public CheckboxPanel(int maximumCheckboxes, boolean selected, Generator<String> text, Generator<ActionListener> action) {
		super();

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(new EmptyBorder(0, 10, 0, 0));

		this.selected = selected;
		boxes = new JCheckBox[maximumCheckboxes];

		for (int i = 0; i < maximumCheckboxes; i++) {
			boxes[i] = new JCheckBox(text.generate(i));
			boxes[i].setSelected(selected);
			boxes[i].addActionListener(action.generate(i));
			this.add(boxes[i]);
		}
	}

	void reset(int activeCheckboxes) {
		for (int i = 0; i < activeCheckboxes; i++) {
			boxes[i].setVisible(true);
			boxes[i].setSelected(selected);
		}
		for (int i = activeCheckboxes; i < boxes.length; i++) {
			boxes[i].setVisible(false);
		}
	}
}

interface Generator<T> {
	T generate(int i);
}
