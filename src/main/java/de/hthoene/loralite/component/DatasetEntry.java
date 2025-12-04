package de.hthoene.loralite.component;

import com.flowingcode.vaadin.addons.imagecrop.ImageCrop;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.server.streams.DownloadHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

@Slf4j
public class DatasetEntry extends HorizontalLayout {

    public DatasetEntry(File imageFile, File captionFile, LogPanel logPanel, Consumer<Void> onRefresh) {
        setWidthFull();
        setAlignItems(Alignment.CENTER);

        Image image = new Image(DownloadHandler.forFile(imageFile), "dataset_image");
        image.setHeight("10rem");
        image.getStyle().set("border-radius", "0.5rem");
        image.addClickListener(event -> openCropDialog(image, imageFile, onRefresh));

        add(image);

        TextArea captionArea = new TextArea("Caption", "Caption here...");
        try {
            captionArea.setValue(Files.readString(captionFile.toPath()));
        } catch (IOException e) {
            logPanel.log(e);
        }
        captionArea.setWidthFull();
        captionArea.setHeight("8rem");
        captionArea.addValueChangeListener(event -> {
            try {
                Files.writeString(
                        captionFile.toPath(),
                        event.getValue(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (IOException e) {
                logPanel.log(e);
            }
        });
        add(captionArea);

        Button deleteButton = new Button("Delete", clickEvent -> openDeleteDialog(imageFile, captionFile, logPanel, onRefresh));
        add(deleteButton);
    }

    private void openCropDialog(Image sourceImage, File imageFile, Consumer<Void> onRefresh) {
        Dialog dialog = new Dialog();
        ImageCrop imageCrop = new ImageCrop(sourceImage);

        dialog.setCloseOnOutsideClick(true);
        dialog.setCloseOnEsc(true);
        dialog.add(imageCrop);
        dialog.open();

        dialog.addDialogCloseActionListener(closeEvent -> {
            try {
                byte[] croppedBytes = imageCrop.getCroppedImageBase64();
                if (croppedBytes != null) {
                    Files.write(imageFile.toPath(), croppedBytes);
                }
            } catch (IOException | IllegalArgumentException e) {
                log.error("Error while cropping image", e);
            } finally {
                onRefresh.accept(null);
                dialog.close();
            }
        });
    }

    private void openDeleteDialog(File imageFile,
                                  File captionFile,
                                  LogPanel logPanel,
                                  Consumer<Void> onRefresh) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Delete file?");
        confirmDialog.setText(imageFile.getName());
        confirmDialog.setCancelable(true);
        confirmDialog.addConfirmListener(confirmEvent -> {
            try {
                Files.deleteIfExists(imageFile.toPath());
                Files.deleteIfExists(captionFile.toPath());
            } catch (IOException e) {
                logPanel.log(e);
                Notification.show("Could not delete files");
                return;
            }
            Notification.show("Delete successful");
            onRefresh.accept(null);
        });
        confirmDialog.open();
    }
}
