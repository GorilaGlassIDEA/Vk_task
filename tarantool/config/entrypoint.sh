#!/bin/bash
set -e
CONTROL_SOCKET="/var/run/tarantool/sys_env/default/instance-001/tarantool.control"

if [ -S "$CONTROL_SOCKET" ]; then
    echo "Removing old control socket..."
    rm -f "$CONTROL_SOCKET"
fi
tarantool --config /etc/tarantool/config.yaml &
TARANTOOL_PID=$!
echo "Waiting for Tarantool..."
while ! timeout 1 bash -c "echo > /dev/tcp/0.0.0.0/3301" 2>/dev/null; do
    sleep 1
done
tarantool -e "
    local net = require('net.box')
    local conn = net.connect('0.0.0.0:3301')
    conn:eval([[
        local s = box.schema.space.create('KV', {if_not_exists = true})
        s:format({
            {name = 'key', type = 'string'},
            {name = 'value', type = 'varbinary', is_nullable = true}
        })
        s:create_index('primary', {type = 'tree', parts = {'key'}, if_not_exists = true})
    ]])
    conn:close()
"

wait $TARANTOOL_PID