package edu.stanford.bmir.protege.web.client.chgpwd;

import edu.stanford.bmir.protege.web.client.Messages;
import edu.stanford.bmir.protege.web.client.dispatch.DispatchServiceCallback;
import edu.stanford.bmir.protege.web.client.dispatch.DispatchServiceManager;
import edu.stanford.bmir.protege.web.client.library.dlg.DialogButton;
import edu.stanford.bmir.protege.web.client.library.dlg.WebProtegeDialog;
import edu.stanford.bmir.protege.web.client.library.dlg.WebProtegeDialogButtonHandler;
import edu.stanford.bmir.protege.web.client.library.dlg.WebProtegeDialogCloser;
import edu.stanford.bmir.protege.web.client.library.msgbox.MessageBox;
import edu.stanford.bmir.protege.web.shared.chgpwd.ResetPasswordAction;
import edu.stanford.bmir.protege.web.shared.chgpwd.ResetPasswordData;
import edu.stanford.bmir.protege.web.shared.chgpwd.ResetPasswordResult;
import edu.stanford.bmir.protege.web.shared.chgpwd.ResetPasswordResultCode;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics Research Group, Date: 01/10/2014
 */
public class ResetPasswordPresenter {

    @Nonnull
    private final DispatchServiceManager dispatchService;

    @Nonnull
    private final Provider<ResetPasswordDialogController> resetPasswordDialogController;

    @Nonnull
    private final Messages messages;

    @Inject
    public ResetPasswordPresenter(@Nonnull DispatchServiceManager dispatchService,
                                  @Nonnull Provider<ResetPasswordDialogController> resetPasswordDialogController,
                                  @Nonnull Messages messages) {
        this.dispatchService = dispatchService;
        this.resetPasswordDialogController = resetPasswordDialogController;
        this.messages = messages;
    }

    public void resetPassword() {
        showDialog();
    }

    private void showDialog() {
        ResetPasswordDialogController controller = resetPasswordDialogController.get();
        controller.setDialogButtonHandler(DialogButton.get(messages.password_resetPassword()), (data, closer) -> {
            closer.hide();
            resetPassword(data);
        });
        WebProtegeDialog<ResetPasswordData> dlg = new WebProtegeDialog<ResetPasswordData>(controller);
        dlg.setVisible(true);
    }

    private void resetPassword(ResetPasswordData data) {
        dispatchService.execute(new ResetPasswordAction(data), new DispatchServiceCallback<ResetPasswordResult>() {

            @Override
            public void handleSuccess(ResetPasswordResult result) {
                if (result.getResultCode() == ResetPasswordResultCode.SUCCESS) {
                    MessageBox.showMessage("Your password has been reset.  " +
                                                   "A temporary password has been sent your email address.");
                }
                else if(result.getResultCode() == ResetPasswordResultCode.INVALID_EMAIL_ADDRESS) {
                    MessageBox.showAlert("Invalid email address.  " +
                                                 "The email address that you supplied is not valid.  " +
                                                 "Your password has not been reset.");
                }
                else {
                    MessageBox.showAlert("Password reset error",
                                         "An error occurred and your password could not be reset.  " +
                                                 "Please contact the administrator.");
                }
            }
        });
    }
}
