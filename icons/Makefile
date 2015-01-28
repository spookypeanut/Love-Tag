CONV := ./makepng
PLAIN := icon-gen-512x512.png icon-gen-144x144.png \
			icon-gen-96x96.png icon-gen-72x72.png \
			icon-gen-48x48.png icon-gen-36x36.png

all : plain
plain : $(PLAIN)

icon-gen-512x512.png : icon.svg
	$(CONV) icon-gen-512x512.png
	optipng -q generated/icon-gen-512x512.png
icon-gen-144x144.png : icon.svg
	$(CONV) icon-gen-144x144.png
	optipng -q generated/icon-gen-144x144.png
icon-gen-96x96.png : icon.svg
	$(CONV) icon-gen-96x96.png
	optipng -q generated/icon-gen-96x96.png
icon-gen-72x72.png : icon.svg
	$(CONV) icon-gen-72x72.png
	optipng -q generated/icon-gen-72x72.png
icon-gen-48x48.png : icon.svg
	$(CONV) icon-gen-48x48.png
	optipng -q generated/icon-gen-48x48.png
icon-gen-36x36.png : icon.svg
	$(CONV) icon-gen-36x36.png
	optipng -q generated/icon-gen-36x36.png

clean : 
	rm -rf generated/*

