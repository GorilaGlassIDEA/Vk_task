box.cfg{
	listen = 3301,
	read_only = false,
}
box.schema.user.grant('guest', 'super', nil, nil, {if_not_exists = true})

box.once('kv_space_init_v1', function()
	local space_name = 'KV'

	local s = box.schema.space.create(space_name, {
		if_not_exists = true,
		engine = 'memtx'
	})
	s:format({
		{name = 'key',     type = 'string'},
		{name = 'value',   type = 'varbinary', is_nullable = true}
	})
	s:create_index('pk', {
		type = 'TREE',
		parts = {{field = 'key', type = 'string'}},
		if_not_exists = true
	})
end)