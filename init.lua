box.cfg{
	listen = 3301,
	read_only = false
}
box.schema.user.grant('guest', 'super', nil, nil, {if_not_exists = true})

box.once('init_v1', function()
	local s = box.schema.space.create('KV', {if_not_exists = true})
	s:format({
		{name = 'key', type = 'string'},
		{name = 'value', type = 'varbinary', is_nullable = true}
	})
	s:create_index('primary', {parts = {'key'}, if_not_exists = true})
	print("--- DATABASE READY AND CONFIGURED ---")
end)