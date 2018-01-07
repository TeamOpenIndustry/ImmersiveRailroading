import bpy

if bpy.context.selected_objects != []:
    for obj in bpy.context.selected_objects:
        if obj.type == 'MESH':
            print(obj.name)
            bpy.context.scene.objects.active = obj
            bpy.ops.object.editmode_toggle()
            bpy.ops.mesh.select_all(action='SELECT')
            bpy.ops.mesh.faces_shade_flat()
            bpy.ops.mesh.remove_doubles()
            bpy.ops.mesh.dissolve_limited(delimit={'SHARP'})
            bpy.ops.mesh.normals_make_consistent()
            bpy.ops.object.editmode_toggle()

