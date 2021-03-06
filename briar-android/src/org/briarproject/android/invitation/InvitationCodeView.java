package org.briarproject.android.invitation;

import static android.view.Gravity.CENTER_HORIZONTAL;

import org.briarproject.R;

import android.content.Context;
import android.widget.TextView;

class InvitationCodeView extends AddContactView implements CodeEntryListener {

	InvitationCodeView(Context ctx) {
		super(ctx);
	}

	void populate() {
		removeAllViews();
		Context ctx = getContext();
		TextView yourCode = new TextView(ctx);
		yourCode.setGravity(CENTER_HORIZONTAL);
		yourCode.setPadding(pad, pad, pad, pad);
		yourCode.setText(R.string.your_invitation_code);
		addView(yourCode);

		TextView code = new TextView(ctx);
		code.setGravity(CENTER_HORIZONTAL);
		code.setTextSize(50);
		code.setPadding(pad, 0, pad, pad);
		int localCode = container.getLocalInvitationCode();
		code.setText(String.format("%06d", localCode));
		addView(code);

		CodeEntryView codeEntry = new CodeEntryView(ctx);
		String enter = container.getString(R.string.enter_invitation_code);
		codeEntry.init(this, enter);
		addView(codeEntry);
	}

	public void codeEntered(int remoteCode) {
		container.remoteInvitationCodeEntered(remoteCode);
	}
}
