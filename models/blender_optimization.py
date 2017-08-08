import bpy

# Inspired by https://blender.stackexchange.com/questions/68093/remove-doubles-on-multiple-objects

if bpy.context.selected_objects != []:
    for obj in bpy.context.selected_objects:
        if obj.type == 'MESH':
            print(obj.name)
            bpy.context.scene.objects.active = obj
            bpy.ops.object.editmode_toggle()
            bpy.ops.mesh.select_all(action='SELECT')
            bpy.ops.mesh.remove_doubles()
            bpy.ops.mesh.tris_convert_to_quads(face_threshold=3.141592653589793, shape_threshold=3.141592653589793, materials=True)
            bpy.ops.object.editmode_toggle()
