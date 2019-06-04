#############################################
# WenQuanYi Bitmap Font Makefile
#
# by Qianqian Fang (fangqq<at>gmail<dot>com)
# 2006/12/13
#############################################
.PHONEY:all clean cjk cjk0 cjka ko ja hangul latin0 cjkl wqyv08 wqyv1 zenhei ttf

B2P=bdftopcf
FONTFORGE=fontforge

SLICE=./bdfmerge.pl  # the shebang line of this script may need adjustment
CJKBASIC=0x201C--0x201C 0x02C7--0x02C9 0x4E00--0x9FCB
CJKEXTA	=0x3400--0x4DB5
CJK1=0x2460--0x24EA 0x2500--0x25EF 0x2605--0x2606 0x2605--0x2606 0x262F--0x2637
CJK2=0x2FF0--0x2FFB 0x3001--0x3020 0x3021--0x3029 0x3105--0x312C 0x3192--0x319F
CJK3=0x31A0--0x31B7 0x3200--0x32FE 0x3300--0x33FE 0xF900--0xFA6A 0xFF01--0xFFFF
NUMBERS=0x0030--0x0039
LATINBASIC=0x0041--0x005A 0x0061--0x007A
LATINPUNC=0x0061--0x007A 0x005B--0x0060 0x007B--0x007E
KOBASIC=0x1100--0x11FF 0x3131--0x318E 0x3200--0x32FE
JABASIC=0x3041--0x3094 0x30A1--0x30FF 0x3200--0x32FE 0x3300--0x33FE
HANGUL=0xAC00--0xD7FF
NOEXTLATIN=-0x00E0--0x0233

FULLRANGE=0x0--0xFFFF
WQYV08=$(FULLRANGE) $(NOEXTLATIN)
WQYV1=$(WQYV08) $(CJKEXTA) -$(HANGUL)
ZENHEI=$(WQYV08) -$(HANGUL)

CJKALL=$(CJKBASIC) $(CJKEXTA) $(CJK1) $(CJK2) $(CJK3) $(KOBASIC) $(JABASIC)
LATINALL=$(NUMBERS) $(LATINBASIC) $(LATINPUNC)
RANGE=$(FULLRANGE)   # default range

#all_bitmap := 9pt 9ptb 10pt 10ptb 11pt 11ptb 12pt 12ptb
all_bitmap := 9pt 10pt 11pt 12pt 13px
all_range  := $(all_bitmap:%=GNU_Unifont_16%)
all_bdf    := $(all_range:%=%.bdf)
all_pcf    := $(all_range:%=%.pcf)

%.pcf: %.bdf
	$(SLICE) $(RANGE) $*.bdf > $*_cjk.bdf
	$(B2P) $*_cjk.bdf > $*.pcf

all: b2p $(all_pcf)
cjk:  RANGE=$(CJKALL)
cjk0: RANGE=$(CJKBASIC)
cjka: RANGE=$(CJKEXTA)
ko:   RANGE=$(KOBASIC)
ja:   RANGE=$(JABASIC)
hangul: RANGE=$(HANGUL)
latin0: RANGE=$(LATINALL)
cjkl:   RANGE=$(CJKALL) $(LATINALL)
wqyv08: RANGE=$(WQYV08)
wqyv1: RANGE=$(WQYV1)
zenhei: RANGE=$(ZENHEI)

cjk cjk0 cjka ko ja hangul latin0 cjkl wqyv08 wqyv1 zenhei: $(all_pcf)

clean:
	-rm -f $(all_pcf) $(all_range:%=%_cjk.bdf)
b2p:
	@if [ -z `which ${B2P}` ]; then echo "Please install bdftopcf to build pcf fonts."; exit 1; fi
ttf: wqyv1
	$(FONTFORGE) -script wqy-bsong-ttf.fe
