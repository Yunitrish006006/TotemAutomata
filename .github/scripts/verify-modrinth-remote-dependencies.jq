(.dependencies | type) == "array"
and (.dependencies | length) == 3
and ([
  .dependencies[]
  | select(
      type == "object"
      and .project_id == $fabric
      and .version_id == null
      and .file_name == null
      and .dependency_type == "required"
    )
] | length) == 1
and ([
  .dependencies[]
  | select(
      type == "object"
      and .project_id == null
      and .version_id == null
      and (.file_name == $core_file or .file_name == null)
      and .dependency_type == "required"
    )
] | length) == 1
and ([
  .dependencies[]
  | select(
      type == "object"
      and .project_id == $excavation
      and .version_id == null
      and .file_name == null
      and .dependency_type == "optional"
    )
] | length) == 1
