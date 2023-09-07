bl_info = {
    "name":         "Animatrix Export",
    "author":       "cam72cam",
    "blender":      (3,4,0),
    "version":      (1,0,4),
    "location":     "File > Import-Export",
    "description":  "Export Animatrix data",
    "category":     "Import-Export",
}

import bpy
from bpy_extras.io_utils import ExportHelper
from bpy.props import StringProperty, BoolProperty, EnumProperty
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

    skip_setting: BoolProperty(
            name="Skip First Frame",
            description="Skip the first animation frame which is typically used as a reference to the exported positions",
            default=False,
    )

    use_setting: EnumProperty(
        name="Export:",
        description="Choose between Scene/Selected",
        items=(
            ('SELECTED', "Selected Objects", "Only export selected objects"),
            ('SCENE', "Entire Scene", "Export all objects in the scene"),
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

                def obj_matrix():
                    res = obj.matrix_world

                    for arm in [m.object for m in obj.modifiers if m.type == "ARMATURE"]:
                        if arm is None:
                            continue
                        return arm.matrix_world @ arm.pose.bones[obj.vertex_groups[0].name].matrix

                    return res


                orig = obj_matrix().inverted()
                for frame in range(bpy.context.scene.frame_start,bpy.context.scene.frame_end + 1):
                    bpy.context.scene.frame_set(frame)

                    offset = obj_matrix() @ orig
                    offset = offset @ (mathutils.Euler((math.radians(90), 0, 0)).to_matrix().to_4x4())
                    m = [offset[0], offset[2], [-z for z in offset[1]], offset[3]]
                    data.append("M " + ",".join(["%.16f" % y for x in m for y in x]) + '\n')
                if len([line for line in data if line != data[0]]) != 0:
                    f.write("O " + obj.name + '\n')
                    f.write("A " + obj.name + "_" + obj.data.name + '\n')
                    if self.skip_setting:
                        del data[0]
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
