SUMMARY = "Device Trees for BSPs"
DESCRIPTION = "Device Tree generation and packaging for BSP Device Trees."
SECTION = "bsp"

LICENSE = "MIT & GPLv2"
LIC_FILES_CHKSUM = " \
		file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302 \
		file://${COMMON_LICENSE_DIR}/GPL-2.0;md5=801f80980d171dd6425610833a22dbe6 \
		"

inherit deploy
inherit kernel-arch

INHIBIT_DEFAULT_DEPS = "1"
DEPENDS += "dtc-native"

PACKAGE_ARCH = "${MACHINE_ARCH}"

FILES_${PN} = "/boot/devicetree/*.dtb /boot/devicetree/*.dtbo"

S = "${WORKDIR}"
B = "${WORKDIR}/build"

SYSROOT_DIRS += "/boot/devicetree"

# By default provide the current kernel arch's boot/dts and boot/dts/include.
KERNEL_DTS_INCLUDE ??= " \
		${STAGING_KERNEL_DIR}/arch/${ARCH}/boot/dts \
		${STAGING_KERNEL_DIR}/arch/${ARCH}/boot/dts/include \
		"
# For arm64/zynqmp the xilinx specific includes are subdired under a vendor directory.
KERNEL_DTS_INCLUDE_append_zynqmp = " \
		${STAGING_KERNEL_DIR}/arch/${ARCH}/boot/dts/xilinx \
		"

DEVICETREE_FLAGS ?= " \
		-R 8 -p 0x3000 -b 0 -i ${S} \
		${@' '.join(['-i %s' % i for i in d.getVar('KERNEL_DTS_INCLUDE', True).split()])} \
		"
DEVICETREE_OFLAGS ?= "-@ -H epapr"
DEVICETREE_PP_FLAGS ?= " \
		-nostdinc -Ulinux -x assembler-with-cpp -I${S} \
		${@' '.join(['-I%s' % i for i in d.getVar('KERNEL_DTS_INCLUDE', True).split()])} \
		"

python () {
    # auto add dependency on kernel tree
    if d.getVar("KERNEL_DTS_INCLUDE") != "":
        d.appendVarFlag("do_compile", "depends", " virtual/kernel:do_shared_workdir")
}

do_compile() {
	for DTS_FILE in ${S}/*.dts; do
		DTS_NAME=`basename -s .dts ${DTS_FILE}`
		${BUILD_CPP} ${DEVICETREE_PP_FLAGS} -o `basename ${DTS_FILE}`.pp ${DTS_FILE}

		# for now use the existance of the '/plugin/' tag to detect overlays
		if grep -qse "/plugin/;" `basename ${DTS_FILE}`.pp; then
			dtc ${DEVICETREE_OFLAGS} -I dts -O dtb ${DEVICETREE_FLAGS} -o ${DTS_NAME}.dtbo `basename ${DTS_FILE}`.pp
		else
			dtc -I dts -O dtb ${DEVICETREE_FLAGS} -o ${DTS_NAME}.dtb `basename ${DTS_FILE}`.pp
		fi
	done
}

do_install() {
	for DTB_FILE in `ls *.dtb *.dtbo`; do
		install -Dm 0644 ${B}/${DTB_FILE} ${D}/boot/devicetree/${DTB_FILE}
	done
}

do_deploy() {
	for DTB_FILE in `ls *.dtb *.dtbo`; do
		install -Dm 0644 ${B}/${DTB_FILE} ${DEPLOYDIR}/${DTB_FILE}
	done
}
addtask deploy before do_build after do_install

