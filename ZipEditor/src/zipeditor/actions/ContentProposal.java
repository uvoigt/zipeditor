package zipeditor.actions;

import org.eclipse.jface.fieldassist.IContentProposal;

// be compatible with release before 3.6
class ContentProposal implements IContentProposal {
	private String content;
	private String label;
	private String description;
	private int cursorPosition;
	public ContentProposal(String content, String label, String description) {
		this.content = content;
		this.label = label;
		this.description = description;
		cursorPosition = content.length();
	}
	public String getContent() {
		return content;
	}
	public int getCursorPosition() {
		return cursorPosition;
	}
	public String getLabel() {
		return label;
	}
	public String getDescription() {
		return description;
	}
}