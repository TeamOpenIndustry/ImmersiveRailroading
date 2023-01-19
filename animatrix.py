bl_info = {
    "name":         "Animatrix Export",
    "author":       "cam72cam",
    "blender":      (3,4,0),
    "version":      (1,0,0),
    "location":     "File > Import-Export",
    "description":  "Export Animatrix data",
    "category":     "Import-Export",
}

import bpy
from bpy_extras.io_utils import ExportHelper
from bpy.props import StringProperty, EnumProperty
from bpy.types import Operator
import math
import mathutils

# Inspired by FriedrichLP's render lib exporter

class ExportAnimatrixData(Operator, ExportHelper):
    """Export Animatrix Data"""
    bl_idname = "animatrix.exporter"
    bl_label = "Export Animatrix"

    # ExportHelper mixin class uses this
    filename_ext = ".anim"

    filter_glob: StringProperty(
        default="*.anim",
        options={'HIDDEN'},
        maxlen=255,  # Max internal buffer length, longer would be clamped.
    )

    use_setting: EnumProperty(
        name="Export:",
        description="Choose between Scene/Selected",
        items=(
            ('SELECTED', "Selected Objects", "Only export selected objects"),
            ('SCENE', "Scene Objects", "Export all objects in the scene"),
        ),
        default='SELECTED',
    )

    def execute(self, context):
        orig_frame = bpy.context.scene.frame_current

        with open(self.filepath, 'w', encoding='utf-8') as f:
            objs = bpy.context.selected_objects if self.use_setting == 'SELECTED' else bpy.context.scene.objects
            for obj in objs:
                if obj.type != 'MESH':
                    continue
                data = []
                bpy.context.scene.frame_set(bpy.context.scene.frame_start)

                orig = obj.matrix_world.inverted()
                for frame in range(bpy.context.scene.frame_start,bpy.context.scene.frame_end + 1):
                    bpy.context.scene.frame_set(frame)

                    offset = obj.matrix_world @ orig
                    offset = offset @ (mathutils.Euler((math.radians(90), 0, 0)).to_matrix().to_4x4())
                    m = [offset[0], offset[2], [-z for z in offset[1]], offset[3]]
                    data.append("M " + ",".join(["%.32f" % y for x in m for y in x]) + '\n')
                if len([line for line in data if line != data[0]]) != 0:
                    f.write("O " + obj.name + '\n')
                    f.write("A " + obj.name + "_" + obj.data.name + '\n')
                    for line in data:
                        f.write(line)

        bpy.context.scene.frame_set(orig_frame)

        return {'FINISHED'}


def menu_func_export(self, context):
    self.layout.operator(ExportAnimatrixData.bl_idname, text="Animatrix (.anim)")


def register():
    bpy.utils.register_class(ExportAnimatrixData)
    bpy.types.TOPBAR_MT_file_export.prepend(menu_func_export)


def unregister():
    bpy.utils.unregister_class(ExportAnimatrixData)
    bpy.types.TOPBAR_MT_file_export.remove(menu_func_export)


if __name__ == "__main__":
    register()
