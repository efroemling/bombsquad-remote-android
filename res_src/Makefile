IMAGE_RESOURCE_INST_DIR = ../app/src/main/res
DRAWABLE_DIR = ../app/src/main/res/drawable

ICON_TARGETS = $(IMAGE_RESOURCE_INST_DIR)/drawable-ldpi/icon.png\
               $(IMAGE_RESOURCE_INST_DIR)/drawable-mdpi/icon.png\
               $(IMAGE_RESOURCE_INST_DIR)/drawable-hdpi/icon.png\
               $(IMAGE_RESOURCE_INST_DIR)/drawable-xhdpi/icon.png\
               $(IMAGE_RESOURCE_INST_DIR)/drawable-xxhdpi/icon.png\
               $(IMAGE_RESOURCE_INST_DIR)/drawable-xxxhdpi/icon.png

OTHER_TARGETS = $(DRAWABLE_DIR)/controllerbg.png\
				$(DRAWABLE_DIR)/button_bomb.png\
				$(DRAWABLE_DIR)/button_bomb_pressed.png\
				$(DRAWABLE_DIR)/button_jump.png\
				$(DRAWABLE_DIR)/button_jump_pressed.png\
				$(DRAWABLE_DIR)/button_throw.png\
				$(DRAWABLE_DIR)/button_throw_pressed.png\
				$(DRAWABLE_DIR)/button_punch.png\
				$(DRAWABLE_DIR)/button_punch_pressed.png \
				$(DRAWABLE_DIR)/center.png\
				$(DRAWABLE_DIR)/thumb.png \
				$(DRAWABLE_DIR)/thumb_pressed.png\
				$(DRAWABLE_DIR)/button_start.png\
				$(DRAWABLE_DIR)/button_leave.png\
				$(DRAWABLE_DIR)/button_settings.png

all: $(ICON_TARGETS) $(OTHER_TARGETS)

# icon files
$(IMAGE_RESOURCE_INST_DIR)/drawable-ldpi/icon.png: Icon.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 36 36 $^ $@
$(IMAGE_RESOURCE_INST_DIR)/drawable-mdpi/icon.png: Icon.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 48 48 $^ $@
$(IMAGE_RESOURCE_INST_DIR)/drawable-hdpi/icon.png: Icon.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 72 72 $^ $@
$(IMAGE_RESOURCE_INST_DIR)/drawable-xhdpi/icon.png: Icon.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 96 96 $^ $@
$(IMAGE_RESOURCE_INST_DIR)/drawable-xxhdpi/icon.png: Icon.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 144 144 $^ $@
$(IMAGE_RESOURCE_INST_DIR)/drawable-xxxhdpi/icon.png: Icon.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 192 192 $^ $@

$(DRAWABLE_DIR)/controllerbg.png: controllerbg.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 1024 512 $^ $@

$(DRAWABLE_DIR)/button_bomb.png: button_bomb.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 256 256 $^ $@
$(DRAWABLE_DIR)/button_bomb_pressed.png: button_bomb_pressed.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 256 256 $^ $@
$(DRAWABLE_DIR)/button_punch.png: button_punch.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 256 256 $^ $@
$(DRAWABLE_DIR)/button_punch_pressed.png: button_punch_pressed.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 256 256 $^ $@
$(DRAWABLE_DIR)/button_throw.png: button_throw.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 256 256 $^ $@
$(DRAWABLE_DIR)/button_throw_pressed.png: button_throw_pressed.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 256 256 $^ $@
$(DRAWABLE_DIR)/button_jump.png: button_jump.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 256 256 $^ $@
$(DRAWABLE_DIR)/button_jump_pressed.png: button_jump_pressed.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 256 256 $^ $@
$(DRAWABLE_DIR)/center.png: center.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 512 512 $^ $@
$(DRAWABLE_DIR)/thumb.png: thumb.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 128 128 $^ $@
$(DRAWABLE_DIR)/thumb_pressed.png: thumb_pressed.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 128 128 $^ $@
$(DRAWABLE_DIR)/button_start.png: button_start.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 128 128 $^ $@
$(DRAWABLE_DIR)/button_leave.png: button_leave.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 128 128 $^ $@
$(DRAWABLE_DIR)/button_settings.png: button_settings.psd
	@echo Creating image: $@
	@./convertUtil convertImageToSize 128 128 $^ $@
