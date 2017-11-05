import bpy

if bpy.context.selected_objects != []:
    for obj in bpy.context.selected_objects:
        if obj.type == 'MESH':
            bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
            bpy.ops.object.editmode_toggle()
            try:
                print(obj.name)
                bpy.context.scene.objects.active = obj
                bpy.ops.mesh.select_all(action='SELECT')
                bpy.ops.mesh.normals_make_consistent()
            except Exception as  ex:
                foo=obj
                print(ex)
                pass
            bpy.ops.object.editmode_toggle()
