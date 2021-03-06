package org.briarproject.android.invitation;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.text.InputType.TYPE_CLASS_NUMBER;
import static android.view.Gravity.CENTER;
import static android.view.Gravity.CENTER_HORIZONTAL;
import static android.view.inputmethod.InputMethodManager.HIDE_IMPLICIT_ONLY;
import static org.briarproject.android.util.CommonLayoutParams.WRAP_WRAP;

import org.briarproject.R;
import org.briarproject.android.util.LayoutUtils;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

class CodeEntryView extends LinearLayout
implements OnEditorActionListener, OnClickListener {

	private final int pad;

	private CodeEntryListener listener = null;
	private EditText codeEntry = null;
	private Button continueButton = null;

	public CodeEntryView(Context ctx) {
		super(ctx);
		pad = LayoutUtils.getPadding(ctx);
	}

	void init(CodeEntryListener listener, String prompt) {
		this.listener = listener;
		setOrientation(VERTICAL);
		setGravity(CENTER_HORIZONTAL);

		Context ctx = getContext();
		TextView enterCode = new TextView(ctx);
		enterCode.setGravity(CENTER_HORIZONTAL);
		enterCode.setPadding(pad, pad, pad, 0);
		enterCode.setText(prompt);
		addView(enterCode);

		LinearLayout innerLayout = new LinearLayout(ctx);
		innerLayout.setOrientation(HORIZONTAL);
		innerLayout.setGravity(CENTER);

		codeEntry = new EditText(ctx) {
			@Override
			protected void onTextChanged(CharSequence text, int start,
					int lengthBefore, int lengthAfter) {
				if(continueButton != null)
					continueButton.setEnabled(getText().length() == 6);
			}
		};
		codeEntry.setId(1); // FIXME: State is not saved and restored
		codeEntry.setTextSize(26);
		codeEntry.setOnEditorActionListener(this);
		codeEntry.setMinEms(5);
		codeEntry.setMaxEms(5);
		codeEntry.setMaxLines(1);
		codeEntry.setInputType(TYPE_CLASS_NUMBER);
		innerLayout.addView(codeEntry);

		continueButton = new Button(ctx);
		continueButton.setLayoutParams(WRAP_WRAP);
		continueButton.setText(R.string.continue_button);
		continueButton.setEnabled(false);
		continueButton.setOnClickListener(this);
		innerLayout.addView(continueButton);
		addView(innerLayout);
	}

	public boolean onEditorAction(TextView textView, int actionId, KeyEvent e) {
		if(!validateAndReturnCode()) codeEntry.setText("");
		return true;
	}

	public void onClick(View view) {
		if(!validateAndReturnCode()) codeEntry.setText("");
	}

	private boolean validateAndReturnCode() {
		String remoteCodeString = codeEntry.getText().toString();
		int remoteCode;
		try {
			remoteCode = Integer.parseInt(remoteCodeString);
		} catch(NumberFormatException e) {
			return false;
		}
		// Hide the soft keyboard
		Object o = getContext().getSystemService(INPUT_METHOD_SERVICE);
		((InputMethodManager) o).toggleSoftInput(HIDE_IMPLICIT_ONLY, 0);
		listener.codeEntered(remoteCode);
		return true;
	}
}
